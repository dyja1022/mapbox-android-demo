package com.mapbox.mapboxandroiddemo.examples.dds;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxandroiddemo.R;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.io.InputStream;

import static com.mapbox.mapboxsdk.style.expressions.Expression.concat;
import static com.mapbox.mapboxsdk.style.expressions.Expression.division;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.product;
import static com.mapbox.mapboxsdk.style.expressions.Expression.round;
import static com.mapbox.mapboxsdk.style.expressions.Expression.string;
import static com.mapbox.mapboxsdk.style.expressions.Expression.subtract;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textSize;

public class TemperatureChangeActivity
  extends AppCompatActivity implements OnMapReadyCallback {

  public static final String GEOJSON_SRC_ID = "extremes_source_id";
  public static final String MIN_TEMP_LAYER_ID = "min_temp_layer_id";
  public static final String MAX_TEMP_LAYER_ID = "max_temp_layer_id";

  private MapboxMap mapboxMap;
  private MapView mapView;
  private FloatingActionButton unitsFab;
  private boolean isImperial = true;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Mapbox access token is configured here. This needs to be called either in your application
    // object or in the same activity which contains the mapview.
    Mapbox.getInstance(this, getString(R.string.access_token));

    // This contains the MapView in XML and needs to be called after the access token is configured.
    setContentView(R.layout.activity_dds_temperature_change);

    unitsFab = findViewById(R.id.change_units_fab);

    mapView = findViewById(R.id.mapView);

    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);

    // When user clicks the map, start the snapshotting process with the given parameters
    unitsFab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

         if (mapboxMap != null) {
           changeTemperatureUnits(!isImperial);
         }
      }
    });

  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {

    this.mapboxMap = mapboxMap;

    // Initialize FeatureCollection object for future use with layers
    FeatureCollection featureCollection =
      FeatureCollection.fromJson(loadGeoJsonFromAsset("weather_data_per_state_before2006.geojson"));

    // Retrieves GeoJSON from local file and adds it to the map
    GeoJsonSource geoJsonSource =
      new GeoJsonSource(GEOJSON_SRC_ID, featureCollection);
    mapboxMap.addSource(geoJsonSource);

    initTemperatureLayers();

  }

  private void initTemperatureLayers() {
    if (mapboxMap != null) {

      // Adds a SymbolLayer to display maximum temperature in state
      SymbolLayer maxTempLayer = new SymbolLayer(MAX_TEMP_LAYER_ID, GEOJSON_SRC_ID);
      maxTempLayer.withProperties(
        textField(getTemperatureValue()),
        textSize(17f),
        textColor(Color.RED),
        textAllowOverlap(true),
        textIgnorePlacement(true)
      );
      // Only display Maximum Temperature in this layer
      maxTempLayer.setFilter(eq(get("element"), literal("All-Time Maximum Temperature")));
      mapboxMap.addLayer(maxTempLayer);

      // Adds a SymbolLayer to display minimum temperature in state
      SymbolLayer minTempLayer = new SymbolLayer(MIN_TEMP_LAYER_ID, GEOJSON_SRC_ID);
      minTempLayer.withProperties(
        textField(getTemperatureValue()),
        textSize(17f),
        textColor(Color.BLUE),
        textAllowOverlap(true),
        textIgnorePlacement(true));
      // Only display Maximum Temperature in this layer
      minTempLayer.setFilter(eq(get("element"), literal("All-Time Minimum Temperature")));
      mapboxMap.addLayer(minTempLayer);
    }
  }

  private void changeTemperatureUnits(boolean isImperial) {
    if (mapboxMap != null && this.isImperial != isImperial) {
      this.isImperial = isImperial;

      // Apply new units to the data displayed in textfields of SymbolLayers
      SymbolLayer maxTempLayer = (SymbolLayer)mapboxMap.getLayer(MAX_TEMP_LAYER_ID);
      maxTempLayer.withProperties(textField(getTemperatureValue()));

      SymbolLayer minTempLayer = (SymbolLayer)mapboxMap.getLayer(MIN_TEMP_LAYER_ID);
      minTempLayer.withProperties(textField(getTemperatureValue()));
    }
  }

  private Expression getTemperatureValue() {

    if (isImperial) {
      return concat(get("value"), literal(" F")); // For imperial we just need to add "F"
    }

    Expression value = Expression.toNumber(get("value"));  // value --> Number
    value = subtract(value, Expression.toNumber(literal(32.0))); // value - 32
    value = product(value, Expression.toNumber(literal(5.0))); // value * 5
    value = division(value, Expression.toNumber(literal(9.0))); // value / 9
    value = round(value); // round to nearest int
    return concat(Expression.toString(value), literal(" C")); // add C at the end
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onPause() {
    mapView.onPause();
    super.onPause();
  }

  @Override
  protected void onStop() {
    mapView.onStop();
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    unitsFab.setOnClickListener(null);
    if (mapboxMap != null) {
      mapboxMap.removeLayer(MAX_TEMP_LAYER_ID);
      mapboxMap.removeLayer(MIN_TEMP_LAYER_ID);
      mapboxMap.removeSource(GEOJSON_SRC_ID);
    }
    mapView.onDestroy();

    super.onDestroy();

    mapView = null;
    mapboxMap = null;
    unitsFab = null;
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  private String loadGeoJsonFromAsset(String filename) {
    try {
      // Load GeoJSON file
      InputStream is = getAssets().open(filename);
      int size = is.available();
      byte[] buffer = new byte[size];
      is.read(buffer);
      is.close();
      return new String(buffer, "UTF-8");

    } catch (Exception exception) {
      Log.e("StyleLineActivity", "Exception Loading GeoJSON: " + exception.toString());
      exception.printStackTrace();
      return null;
    }
  }

}
