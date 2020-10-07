package com.example.musicplayer_try1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jean.jcplayer.model.JcAudio;
import com.example.jean.jcplayer.view.JcPlayerView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private boolean checkpermission = false;
    Uri uri;
    String BaniName,BaniUrl;
    ListView listView;

    ArrayList<String> arrayListBaniName=new ArrayList<>();
    ArrayList<String> arrayListBaniUrl=new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;

  JcPlayerView jcPlayerView;
  ArrayList<JcAudio> jcAudios = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView=findViewById(R.id.myListview);

        jcPlayerView=findViewById(R.id.jcplayer);

        retreieveBanis();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                
                jcPlayerView.playAudio(jcAudios.get(position));
                jcPlayerView.setVisibility(View.VISIBLE);

                jcPlayerView.createNotification();

            }
        });


    }

    private void retreieveBanis()
    {
        DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference("Banis");

        databaseReference.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                for(DataSnapshot ds :snapshot.getChildren())
                {
                    Bani baniobj = ds.getValue(Bani.class);
                    arrayListBaniName.add(baniobj.getBaniName());
                    arrayListBaniUrl.add(baniobj.getBaniUrl());

                    jcAudios.add(JcAudio.createFromURL(baniobj.getBaniName(),baniobj.getBaniUrl()));


                }

                arrayAdapter=new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_1,arrayListBaniName)
                {
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
                    {
                        View view =super.getView(position,convertView,parent);
                        TextView textview=(TextView)view.findViewById(android.R.id.text1);

                        textview.setSingleLine(true);
                        textview.setMaxLines(1);

                        return view;
                    }
                };

                jcPlayerView.initPlaylist(jcAudios,null);

                listView.setAdapter(arrayAdapter);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {

            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.custom_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.bani_upload) {
            if (validatepermission()) {
                pickbani();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean validatepermission()
    {
        Dexter.withActivity(MainActivity.this)                                          //why .withactivity is cut in between
            .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            .withListener(new PermissionListener() {
                @Override
                public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                    checkpermission = true;
                }

                @Override
                public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                    checkpermission = false;
                }

                @Override
                public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                    permissionToken.continuePermissionRequest();
                }
            }).check();
        return checkpermission;
    }


    private void pickbani() {
        Intent intent_upload = new Intent();
        intent_upload.setType("audio/*");
        intent_upload.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent_upload, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                uri = data.getData();
                Cursor mcursor = getApplicationContext().getContentResolver()
                        .query(uri, null, null, null, null);

                int indexedname = mcursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                mcursor.moveToFirst();
                BaniName = mcursor.getString(indexedname);
                mcursor.close();

                uploadBanitoFirebaseStorage();


            }

            super.onActivityResult(requestCode, resultCode, data);
        }


    }

    private void uploadBanitoFirebaseStorage()
    {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("Bani").child(uri.getLastPathSegment());

        final ProgressDialog progressDialog=new ProgressDialog(this);
        progressDialog.show();

        storageReference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
        {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
            {
               Task<Uri> uriTask=taskSnapshot.getStorage().getDownloadUrl();
               while(!uriTask.isComplete());
               Uri urlbani=uriTask.getResult();
               BaniUrl=urlbani.toString();

               uploaddetailstoDatabase();
               progressDialog.dismiss();

            }
        }).addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                Toast.makeText(MainActivity.this,e.getMessage().toString(),Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>()
        {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot)
            {
                double progress = (100.0 * snapshot.getBytesTransferred())/snapshot.getTotalByteCount();
                int currentprogress=(int)progress;
                progressDialog.setMessage("Uploaded"+currentprogress+"%");
            }
        });



    }

    private void uploaddetailstoDatabase()
    {

     Bani Baniobj=new Bani(BaniName,BaniUrl);

        FirebaseDatabase.getInstance().getReference("Bani")
                .push().setValue(Baniobj).addOnCompleteListener(new OnCompleteListener<Void>()
        {
            @Override
            public void onComplete(@NonNull Task<Void> task)
            {
             if(task.isSuccessful())
             {
                 Toast.makeText(MainActivity.this,"Bani Uploaded",Toast.LENGTH_LONG).show();
             }
            }
        }).addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                Toast.makeText(MainActivity.this,e.getMessage().toString(),Toast.LENGTH_LONG).show();
            }
        });
    }


}