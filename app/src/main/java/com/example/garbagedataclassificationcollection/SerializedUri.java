package com.example.garbagedataclassificationcollection;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.io.Serializable;

public class SerializedUri implements Serializable {
    private final Uri uri;
    public SerializedUri(Uri uri){
        this.uri = uri;
    }
    public DocumentFile getDocumentFileFromUri(Context ctx){
        return DocumentFile.fromSingleUri(ctx, uri);
    }
}
