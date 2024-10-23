package com.example.garbagedataclassificationcollection;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_WRITE_SOTRAGE = 1;
    private static final int REQUEST_READ_SOTRAGE = 2;

    private TextView pathTxt;
    private ActivityResultLauncher<Intent> document_tree_launcher;
    DocumentFile myDataSetDirectory;
    DocumentFile myDataSet;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        pathTxt = findViewById(R.id.path_txt);

        findViewById(R.id.browse_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askForReadPermission();
            }
        });

        document_tree_launcher =registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result->{
            if(result.getResultCode() == RESULT_OK){
                Intent data = result.getData();
                if(data!=null){
                    Uri treeUri = handleDirectory(data);
                     myDataSetDirectory= DocumentFile.fromTreeUri(this, treeUri);
                     // get CSV Document
                    boolean foundCsv=false;

                    DocumentFile[] files = myDataSetDirectory.listFiles();
                    int i=0;
                    while(!foundCsv && i<files.length){
                        if(files[i].isFile() && files[i].getName()!=null && files[i].getName().endsWith(".csv")){
                            // found it!!
                            foundCsv=true;
                            myDataSet = files[i];

                        }
                        i++;
                    }
                    Toast.makeText(this,"File found = " +foundCsv, Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @NonNull
    private Uri handleDirectory(@NonNull Intent data){
        Uri uri = data.getData();
        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        pathTxt.setText(uri.toString());
        return uri;
    }

    private void browseFolder(){
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        document_tree_launcher.launch(i);
    }


    private void askForReadPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Request the permission if not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_SOTRAGE);

        } else {
            askForWritePermission();
        }
    }

    private void askForWritePermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Request the permission if not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_SOTRAGE);

        } else {
            browseFolder();
        }
    }
}