package com.example.gaitanalyzer;

import android.view.KeyEvent;

import com.aditya.filebrowser.FileChooser;

public class FileChooserMod extends FileChooser {
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

}