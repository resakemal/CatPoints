package resakemal.catpoints.Controller;

/**
 * Created by Resa Kemal Saharso on 02/12/2017.
 */

import android.content.ComponentName;
import android.content.Context;
import android.location.Location;
import android.media.Image;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import resakemal.catpoints.R;

public class DatabaseManager {

    private DatabaseReference mLocationReference;
    private GoogleMap mMap;
    private int locNum;
    private ArrayList<LatLng> locList;
    private ArrayList<Marker> markerList;

    public DatabaseManager() {
        mLocationReference = FirebaseDatabase.getInstance().getReference().child("location");
        mMap = null;
        locList = new ArrayList<>();
        markerList = new ArrayList<>();
    }

    public DatabaseManager(GoogleMap _mMap) {
        mLocationReference = FirebaseDatabase.getInstance().getReference().child("location");
        mMap = _mMap;
        locList = new ArrayList<>();
        markerList = new ArrayList<>();
    }

    public void setupListener() {
        ValueEventListener locListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int i = 0;
                locNum = 0;
                String idx = Integer.toString(i);

                mMap.clear();
                while (dataSnapshot.hasChild(idx)) {
                    double lat = Double.parseDouble((String) dataSnapshot.child(idx).child("lat").getValue());
                    double lng = Double.parseDouble((String) dataSnapshot.child(idx).child("lng").getValue());
                    LatLng inst = new LatLng(lat,lng);
                    locList.add(inst);
                    Marker new_marker = mMap.addMarker(new MarkerOptions().position(inst).icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_kucing)));
                    markerList.add(i,new_marker);

                    i++;
                    idx = Integer.toString(i);
                }
                locNum = i;
                Log.d("locNum", Integer.toString(markerList.size()));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        mLocationReference.addValueEventListener(locListener);
    }

    public ArrayList<LatLng> getLocList() {
        return this.locList;
    }


    public void createNewEntry(Location pos, String photo_dir) {
        Map<String,String> insert = new HashMap<>();
        insert.put("lat",Double.toString(pos.getLatitude()));
        insert.put("lng",Double.toString(pos.getLongitude()));
        mLocationReference.child(Integer.toString(locNum)).setValue(insert);

        Uri file = Uri.fromFile(new File(photo_dir));
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("images").child(Integer.toString(locNum) + ".jpg");
        UploadTask uploadTask = storageRef.putFile(file);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Log.d("upload_photo","fail");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                Log.d("upload_photo","success");
            }
        });

        locNum++;
    }

    public void getCatPhoto(Context context, Marker marker, final ImageView cat_view) {
        cat_view.setClickable(true);

        String path = "images/" + Integer.toString(markerList.indexOf(marker)) + ".jpg";

        Log.d("marker_photo",path);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(path);
//        storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
//            @Override
//            public void onSuccess(Uri uri) {
//                // Got the download URL for 'users/me/profile.png'
//                cat_view.setImageDrawable(null);
//                cat_view.setImageURI(uri);
//                Log.d("marker_photo","success");
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception exception) {
//                // Handle any errors
//                StringWriter sw = new StringWriter();
//                exception.printStackTrace(new PrintWriter(sw));
//                String exceptionAsString = sw.toString();
//                Log.d("marker_photo",exceptionAsString);
//            }
//        });

        Glide.with(context)
                .using(new FirebaseImageLoader())
                .load(storageRef)
                .into(cat_view);
    }
}
