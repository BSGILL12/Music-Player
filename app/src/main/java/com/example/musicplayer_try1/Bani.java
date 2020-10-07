package com.example.musicplayer_try1;

public class Bani
{
 private String BaniName,BaniUrl;

    public Bani()
    {
    }

    public Bani(String baniName, String baniUrl)
    {
        BaniName = baniName;
        BaniUrl = baniUrl;
    }

    public String getBaniName()
    {
        return BaniName;
    }

    public void setBaniName(String baniName)
    {
        BaniName = baniName;
    }

    public String getBaniUrl()
    {
        return BaniUrl;
    }

    public void setBaniUrl(String baniUrl)
    {
        BaniUrl = baniUrl;
    }
}
