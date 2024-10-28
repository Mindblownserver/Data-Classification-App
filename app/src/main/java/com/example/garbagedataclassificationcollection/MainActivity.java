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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.floatingactionbutton.FloatingActionButton;


import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_WRITE_SOTRAGE = 1;
    private static final int REQUEST_READ_SOTRAGE = 2;

    private TextView pathTxt;
    private ActivityResultLauncher<Intent> document_tree_launcher;
    private DocumentFile myDataSetDirectory;
    private DocumentFile myDataSet;
    private FloatingActionButton camBtn;
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
        camBtn = findViewById(R.id.cam_btn);

        findViewById(R.id.browse_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askForReadPermission();
            }
        });

        camBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // turn this into a launcher, then this will get the byte[] or buffer and write it into the storage (and changes the csv file)
                Intent i = new Intent(MainActivity.this, CameraFeedActivity.class);

                startActivity(i);
            }
        });

        document_tree_launcher =registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result->{
            if(result.getResultCode() == RESULT_OK){
                Intent data = result.getData();
                if(data!=null){
                    Uri treeUri = handleDirectory(data);
                    myDataSetDirectory= DocumentFile.fromTreeUri(this, treeUri);
                    // check if you can write in directory
                    // get CSV Document
                    boolean foundFile = csvFileExists();
                    if(!foundFile){
                        myDataSet = myDataSetDirectory.createFile("text/csv", "myDataSet.csv");
                        writeHeader(myDataSet);
                    }
                    pathTxt.setText(R.string.dir_loaded);
                }
            }
        });

    }



    private boolean csvFileExists(){
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
        return foundCsv;
    }

    private void writeHeader(DocumentFile csvFile){
        if(myDataSet!=null && myDataSet.canWrite()){
            try{
                OutputStream out = getContentResolver().openOutputStream(myDataSet.getUri());
                String initData = "garbage_class, image";
                out.write(initData.getBytes());
                out.close();
                Toast.makeText(this, "Wrote File successfully", Toast.LENGTH_SHORT).show();
            }catch(IOException e){
                Toast.makeText(this, ""+e, Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void writeData(DocumentFile csvFile){
        if(myDataSet!=null && myDataSet.canWrite()){
            try{
                OutputStream out = getContentResolver().openOutputStream(myDataSet.getUri(), "wa");
                String initData = "\ngarbage_class, image";
                out.write(initData.getBytes());
                out.close();
                Toast.makeText(this, "Wrote File successfully", Toast.LENGTH_SHORT).show();
            }catch(IOException e){
                Toast.makeText(this, ""+e, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @NonNull
    private Uri handleDirectory(@NonNull Intent data){
        Uri uri = data.getData();
        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
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