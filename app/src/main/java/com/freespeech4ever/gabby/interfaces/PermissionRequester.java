package com.freespeech4ever.gabby.interfaces;

public interface PermissionRequester {
    void onRequestPermissionsResult(String[] permissions, int[] grantResults);
}