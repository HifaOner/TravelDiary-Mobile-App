package com.info.javamaps.view;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.info.javamaps.R;
import com.info.javamaps.databinding.ActivityMapsBinding;
import com.info.javamaps.model.Place;
import com.info.javamaps.roomdb.PlaceDao;
import com.info.javamaps.roomdb.PlaceDatabase;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    ActivityResultLauncher<String> permissionLauncher; // -------------izni istemek için,konum verilince napıcaz

    LocationManager locationManager;
    LocationListener locationListener;

    SharedPreferences sharedPreferences;
    boolean trackBoolen;

    //Seçilen enlem ve boylam
    Double selectedLatitude;
    Double selectedLongitude;

    //ROOM DATABASE INITIALIZE ETME
    PlaceDatabase db;
    PlaceDao placeDao;
    //Rx Java
    private final CompositeDisposable mDisposable = new CompositeDisposable();  //Kullan-at çöpe at

    Place placeFromMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        registerLauncher();  //izin verildi mi verilmedi mi

        sharedPreferences = MapsActivity.this.getSharedPreferences("com.info.javamaps",MODE_PRIVATE);
        trackBoolen = false;

        binding.saveButton.setEnabled(false);

        selectedLatitude = 0.0;
        selectedLongitude = 0.0;

        //ROOM DATABASE INITIALIZE ETME
        db = Room.databaseBuilder(getApplicationContext(),
                        PlaceDatabase.class,"Places")
                        //.allowMainThreadQueries()
                        .build();
        placeDao = db.placeDao();



    }


    @Override
    public void onMapReady(GoogleMap googleMap) {  //HARİTANIN BAŞLANGIÇ KONUMUNU BELİRTME --1.DERS--
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this); //oluşturduğum arayüzü,gücel haritamda kullanacağımı söylüyorum

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if (info.equals("new")) {
            binding.saveButton.setVisibility(View.VISIBLE);
            binding.deleteButton.setVisibility(View.GONE);

            //casting (eminim abi demek)
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE); //Konum yöneticimiz
            locationListener = new LocationListener() {  // konum yöneticisi değişiklikleri dinliyor ve buradan da yapmamız gerekenleri yapıyoruz
                @Override
                public void onLocationChanged(@NonNull Location location) {  //konum değişirse ne olacağını yazıyoruz  //BURADA HİÇBİR ŞEY YAPMASAK DA OLUR

                    // Kameramızı rahatça değiştirmek için çözüm; // Eğer lokasyon değişmediyse bir şey yapmıyoruz
                    //SharedPreferences sharedPreferences = MapsActivity.this.getSharedPreferences("com.info.javamaps",MODE_PRIVATE);
                    trackBoolen = sharedPreferences.getBoolean("trackBoolean", false); //sahredPref de  kaydettik

                    if (!trackBoolen) {  //eğer lokasyon değiştiyse artık kaydediyoruz ama sadece 1 kez gerçekleşiyor
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());  // kullanıcını bulunduğu enlem ve boylam
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));  //kamerayı konumun değiştiği yere götür
                        sharedPreferences.edit().putBoolean("trackBoolean", true).apply();
                    }
                }
            };

            // KONUM İZNİ ALMAK


                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        Snackbar.make(binding.getRoot(), "Permisson Needed for maps", Snackbar.LENGTH_INDEFINITE).setAction("Give Permisson", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //request permisson
                                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                            }
                        }).show();
                    } else {
                        //request permisson
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                    }
                } else {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener); //KONUM DEĞİŞİKLERİNİ AL; LISTENER DINLESİN

                    Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);  // SON KONUMU ALDIK
                    if (lastLocation != null) {
                        LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15));
                    }
                    mMap.setMyLocationEnabled(true); // konumumuzu mavi işaretle gösterir
                }


            } else {
                //Sqlite data && intent data
                mMap.clear();
                placeFromMain = (Place) intent.getSerializableExtra("place");

                LatLng latLng = new LatLng(placeFromMain.latitude, placeFromMain.longitude);

                mMap.addMarker(new MarkerOptions().position(latLng).title(placeFromMain.name));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

                binding.placeNameText.setText(placeFromMain.name);
                binding.saveButton.setVisibility(View.GONE);
                binding.deleteButton.setVisibility(View.VISIBLE);
            }


    }



        //latiude(enlem), longitude(boylam)  => LatLng(enlem-boylam sınıfı)
        // Add a marker in Paris and move the camera
        //LatLng eiffel = new LatLng(48.8583701,2.2922926);  // Eiffel Tower Enlem Boylam = 48.8583701,2.2922926,17
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eiffel,15)); //Kamerayı Paris'e Odakladık ve zoom ayarladık
        //mMap.addMarker(new MarkerOptions().position(eiffel).title("Eiffel Tower"));  //Konum işaretçisi ve başlığı ekledik




    private void registerLauncher(){
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) { // izin verildi mi
                     if (result){
                         //permission granted izin verildi
                         if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                             locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener); //kullanıcıdan konumu aldık

                             // KULLANICININ SON BİLİNEN KONUMU VARSA MAP ORADAN BAŞLATILIR
                             Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);  // SON KONUMU ALDIK
                             if (lastLocation != null){ //eğer son konum varsa
                                 LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude()); // son konumunu tanımladık
                                 mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15));  // son konumu gösterdik ve zoomladık
                             }
                         }

                     } else {
                         //permission denied
                         Toast.makeText(MapsActivity.this, "Permission needed!", Toast.LENGTH_SHORT).show();
                     }
            }
        });
    }




    @Override
    public void onMapLongClick(LatLng latLng) {  // Uzun Tıkladığımızda Nolcak
        mMap.clear(); // öncesinde marker varsa siliyoruz
        mMap.addMarker(new MarkerOptions().position(latLng));  // uzun tıkladığımızda marker koycak

        selectedLatitude = latLng.latitude;
        selectedLongitude = latLng.longitude;

        binding.saveButton.setEnabled(true);

    }



    public void save(View view){
        Place place = new Place(binding.placeNameText.getText().toString(),selectedLatitude,selectedLongitude);
        //placeDao.insert(place);


        //threading ==> Main (UI), Default(CPU Instensive), IO (network,database)
        //placeDao.insert(place).subscribeOn(Schedulers.io()).subscribe();

        //disposable
        mDisposable.add(placeDao.insert(place)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MapsActivity.this::handleResponse)
        );
    }

    private void handleResponse(){
        Intent intent = new Intent(MapsActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void delete(View view) {

        if (placeFromMain != null) {
        mDisposable.add(placeDao.delete(placeFromMain)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MapsActivity.this::handleResponse));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDisposable.clear();
    }
}