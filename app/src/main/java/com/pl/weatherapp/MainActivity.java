package com.pl.weatherapp;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.UrlRewriter;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.pl.weatherapp.databinding.ActivityMainBinding;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private ArrayList<WeatherRVModel> weatherRVModelArrayList;
    private WeatherRVAdapter weatherRVAdapter;
    private LocationManager locationManager;
    private int PERMISSION_CODE = 1;
    private String cityName;
    Location location;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String currentDateAndTime = getCurrentDateAndTime();
        binding.currentDate.setText(currentDateAndTime);

        weatherRVModelArrayList = new ArrayList<>();
        weatherRVAdapter = new WeatherRVAdapter(this, weatherRVModelArrayList);
        binding.RVWeather.setAdapter(weatherRVAdapter);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        } else {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                cityName = getCityName(location.getLongitude(), location.getLatitude());
                Log.d(TAG, "onCreate: location: cityName: " + cityName);
                Log.d(TAG, "onCreate: location: " + location);
                getWeatherInfo(cityName);
            } else {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
            }
        }


        binding.IVSearch.setOnClickListener(v -> {
            String city = binding.EdtCity.getText().toString();
            if (city.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please Enter City Name..", Toast.LENGTH_SHORT).show();
            } else {
                String capitalizedCity = capitalizeFirstLetter(city);
                binding.TVCityName.setText(capitalizedCity);
                Log.d(TAG, "onCreate: searchTv: cityName: " + capitalizedCity);
                getWeatherInfo(capitalizedCity);
            }
        });
    }

    private String getCurrentDateAndTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String capitalizeFirstLetter(String city) {
        if (city == null || city.isEmpty()) {
            return city;
        }
        return city.substring(0, 1).toUpperCase() + city.substring(1).toLowerCase();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions Granted..", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please Provide The Permissions", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private String getCityName(double longitude, double latitude) {
        String cityName = "No found";
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        try {
            List<Address> addresses = gcd.getFromLocation(latitude, longitude, 10);

            for (Address adr : addresses) {
                if (adr != null) {
                    String city = adr.getLocality();
                    if (city != null && !city.equals("")) {
                        cityName = city;
                    } else {
                        Log.d("Tag", "City Not Found");
                        Toast.makeText(this, "User City Not Found..", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;
    }

    private void getWeatherInfo(String cityName) {
        Log.d(TAG, "getCityName: cityName: cityName: " + cityName);

        String url = "http://api.weatherapi.com/v1/forecast.json?key=2bc844ec7dc24b9384694214231209&q=" + cityName + "&days=1&aqi=yes&alerts=yes";
        Log.d(TAG, "getWeatherInfo: url: url: " + url);

        binding.TVCityName.setText(cityName);
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        Log.d(TAG, "getWeatherInfo: " + requestQueue);

        binding.progressBar.setVisibility(View.VISIBLE);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "getWeatherInfo: onResponse: response: " + response);
                binding.RLHome.setVisibility(View.VISIBLE);
                weatherRVModelArrayList.clear();

                try {
                    String temperature = response.getJSONObject("current").getString("temp_c");
                    binding.TVTemperature.setText(temperature + "°");
                    int isDay = response.getJSONObject("current").getInt("is_day");
                    String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    String feelsLike = response.getJSONObject("current").getString("feelslike_c");
                    String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    Picasso.get().load("https:".concat(conditionIcon)).into(binding.IVIcon);
                    binding.TVCondition.setText(condition);
                    binding.TVFeelsLike.setText("Feels Like " + feelsLike + "°");

                    JSONObject forecastObj = response.getJSONObject("forecast");
                    JSONObject forecastO = forecastObj.getJSONArray("forecastday").getJSONObject(0);

                    String maxTemp = forecastO.getJSONObject("day").getString("maxtemp_c");
                    String minTemp = forecastO.getJSONObject("day").getString("mintemp_c");
                    binding.maxTemp.setText("Max: " + maxTemp + "°");
                    binding.minTemp.setText("Min: " + minTemp + "°");

                    String windSpeed = response.getJSONObject("current").getString("wind_kph");
                    binding.windSpeed.setText(windSpeed + " km/h");
                    binding.constraintLayoutWindSpeed.setVisibility(View.VISIBLE);
                    String rainChance = forecastO.getJSONObject("day").getString("daily_chance_of_rain");
                    binding.rainChance.setText(rainChance + "%");
                    binding.constraintLayoutRainChance.setVisibility(View.VISIBLE);
                    String sunrise = forecastO.getJSONObject("astro").getString("sunrise");
                    String sunset = forecastO.getJSONObject("astro").getString("sunset");
                    binding.sunriseTime.setText(sunrise);
                    binding.sunsetTime.setText(sunset);
                    binding.constraintLayoutSunrise.setVisibility(View.VISIBLE);
                    binding.constraintLayoutSunset.setVisibility(View.VISIBLE);

                    JSONArray hourArray = forecastO.getJSONArray("hour");
                    for (int i = 0; i < hourArray.length(); i++) {
                        JSONObject hourObj = hourArray.getJSONObject(i);
                        String time = hourObj.getString("time");
                        String temper = hourObj.getString("temp_c");
                        String img = hourObj.getJSONObject("condition").getString("icon");
                        String wind = hourObj.getString("wind_kph");
                        weatherRVModelArrayList.add(new WeatherRVModel(time, temper, img, wind));
                        binding.constraintLayoutHourlyForecast.setVisibility(View.VISIBLE);
                    }

                    weatherRVAdapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    binding.progressBar.setVisibility(View.GONE);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Please Enter Valid City Name", Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(jsonObjectRequest);
    }
}