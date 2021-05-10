package com.hllbr.mycoinbook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class coinDetailActivity extends AppCompatActivity {
    EditText coinNameText,CoinCeoText,CoinProjectText;
    ImageView imageView;
    Button button;
    Bitmap selectedCoin;
    SQLiteDatabase database ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coin_detail);

        coinNameText = findViewById(R.id.coinNameText);
        CoinCeoText = findViewById(R.id.ceoNameText);
        CoinProjectText = findViewById(R.id.projectNameText);
        imageView = findViewById(R.id.imageView);
        button = findViewById(R.id.button1);
        database = this.openOrCreateDatabase("Coins",MODE_PRIVATE,null);

        Intent intent= getIntent();
        String info = intent.getStringExtra("info");
        if(info.matches("new")){
            coinNameText.setText("");
            CoinProjectText.setText("");
            CoinCeoText.setText("");
            button.setVisibility(View.VISIBLE);

            Bitmap selectCoin = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.selectedimage1);
            imageView.setImageBitmap(selectCoin);
        }else{
            int coinId = intent.getIntExtra("coinId",1);
            button.setVisibility(View.INVISIBLE);
            try{
                Cursor cursor = database.rawQuery("SELECT * FROM coins WHERE id = ?",new String[]{String.valueOf(coinId)});
                int coinNameIx = cursor.getColumnIndex("name");
                int ceoNameIx = cursor.getColumnIndex("ceo");
                int projectIx = cursor.getColumnIndex("project");
                int imageIx = cursor.getColumnIndex("image");

                while(cursor.moveToNext()){
                    coinNameText.setText(cursor.getString(coinNameIx));
                    CoinCeoText.setText(cursor.getString(ceoNameIx));
                    CoinProjectText.setText(cursor.getString(projectIx));

                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    imageView.setImageBitmap(bitmap);
                }
                cursor.close();
            }catch (Exception ex){

            }
        }
    }
    public void selectedCoin(View view){
        Intent intent= getIntent();
        String info = intent.getStringExtra("info");
        if(info.matches("new")){

            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

            }else{
                Intent intentToGallery =new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentToGallery,2);
            }
        }else{
            Toast.makeText(this,"you don't have a trace",Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 1){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent intentToGallery = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentToGallery,2);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == 2 && resultCode == RESULT_OK && data != null){
         Uri imageData = data.getData();

            try {
                if(Build.VERSION.SDK_INT >= 28){
                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),imageData);
                    selectedCoin = ImageDecoder.decodeBitmap(source);
                    imageView.setImageBitmap(selectedCoin);
                }else{
                    selectedCoin = MediaStore.Images.Media.getBitmap(this.getContentResolver(),imageData);
                    imageView.setImageBitmap(selectedCoin);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void save(View view){
        String coinName = coinNameText.getText().toString();
        String coinCeoName = CoinCeoText.getText().toString();
        String projectString = CoinProjectText.getText().toString();

        Bitmap smallImage =makeSmallerImage(selectedCoin,300);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray= outputStream.toByteArray();
        try {
            database = this.openOrCreateDatabase("Coins",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS coins(id INTEGER PRIMARY KEY,name VARCHAR,ceo VARCHAR,project VARCHAR,image BLOB)");
            String sqlString = "INSERT INTO coins(name,ceo,project,image) VALUES (?,?,?,?)";
            SQLiteStatement sqLiteStatement =database.compileStatement(sqlString);
            //bir stringin sql içerisinde sql komut gibi çalıştırılmasına olanak sağlıyor.
            sqLiteStatement.bindString(1,coinName);
            sqLiteStatement.bindString(2,coinCeoName);
            sqLiteStatement.bindString(3,projectString);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();
        }catch (Exception ex){

        }
        Intent intent = new Intent(coinDetailActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }
    public Bitmap makeSmallerImage(Bitmap image,int maxsimumsize){
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float)width/(float)height;
        if(bitmapRatio>1){
            width = maxsimumsize;
            height = (int)(width/bitmapRatio);
        }else{
            height = maxsimumsize;
            width = (int)(height*bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image,width,height,true);

    }
}