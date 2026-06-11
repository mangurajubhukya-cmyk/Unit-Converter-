package com.example.unitconverter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Spinner spinnerCategory, spinnerFrom, spinnerTo, spinnerDialogCategory;
    private EditText etValue, etUnitName, etUnitFactor;
    private Button btnConvert, btnAddUnit, btnHistory;
    private ImageButton btnSwap;
    private TextView tvResult;

    // Units definitions (unit display -> factor to base unit)
    // Length base: meter
    private final LinkedHashMap<String, Double> lengthUnits = new LinkedHashMap<>();
    // Mass base: kilogram
    private final LinkedHashMap<String, Double> massUnits = new LinkedHashMap<>();
    // Volume base: liter
    private final LinkedHashMap<String, Double> volumeUnits = new LinkedHashMap<>();
    // Speed base: meter per second
    private final LinkedHashMap<String, Double> speedUnits = new LinkedHashMap<>();
    // Data Size base: byte
    private final LinkedHashMap<String, Double> dataSizeUnits = new LinkedHashMap<>();
    // Area base: square meter
    private final LinkedHashMap<String, Double> areaUnits = new LinkedHashMap<>();
    // Time base: second
    private final LinkedHashMap<String, Double> timeUnits = new LinkedHashMap<>();
    // Pressure base: pascal
    private final LinkedHashMap<String, Double> pressureUnits = new LinkedHashMap<>();

    // Temperature display names (we map to canonical keys)
    private final String[] temperatureUnits = {"Celsius (°C)", "Fahrenheit (°F)", "Kelvin (K)"};

    // Persistence keys
    private static final String PREFS = "unitconverter_prefs";
    private static final String KEY_CUSTOM_UNITS = "custom_units_json";
    private static final String KEY_HISTORY = "history_json";

    private SharedPreferences prefs;

    // History list in memory
    private final ArrayList<String> history = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
        etValue = findViewById(R.id.etValue);
        btnConvert = findViewById(R.id.btnConvert);
        btnAddUnit = findViewById(R.id.btnAddUnit);
        btnHistory = findViewById(R.id.btnHistory);
        btnSwap = findViewById(R.id.btnSwap);
        tvResult = findViewById(R.id.tvResult);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        initUnits();
        loadCustomUnits();
        loadHistory();
        setupCategorySpinner();

        btnConvert.setOnClickListener(v -> onConvert());
        btnAddUnit.setOnClickListener(v -> showAddUnitDialog());
        btnHistory.setOnClickListener(v -> showHistoryDialog());
        btnSwap.setOnClickListener(v -> onSwap());

        tvResult.setOnClickListener(v -> copyResultToClipboard());
    }

    private void initUnits() {
        // Length units -> factor to meter
        lengthUnits.put("Millimeter (mm)", 0.001);
        lengthUnits.put("Centimeter (cm)", 0.01);
        lengthUnits.put("Meter (m)", 1.0);
        lengthUnits.put("Kilometer (km)", 1000.0);
        lengthUnits.put("Inch (in)", 0.0254);
        lengthUnits.put("Foot (ft)", 0.3048);
        lengthUnits.put("Yard (yd)", 0.9144);
        lengthUnits.put("Mile (mi)", 1609.34);

        // Mass units -> factor to kilogram
        massUnits.put("Milligram (mg)", 0.000001);
        massUnits.put("Gram (g)", 0.001);
        massUnits.put("Kilogram (kg)", 1.0);
        massUnits.put("Tonne (t)", 1000.0);
        massUnits.put("Ounce (oz)", 0.0283495);
        massUnits.put("Pound (lb)", 0.453592);

        // Volume units -> factor to liter
        volumeUnits.put("Milliliter (mL)", 0.001);
        volumeUnits.put("Liter (L)", 1.0);
        volumeUnits.put("Cubic meter (m³)", 1000.0);
        volumeUnits.put("Teaspoon (tsp)", 0.00492892);
        volumeUnits.put("Tablespoon (tbsp)", 0.0147868);
        volumeUnits.put("Fluid ounce (fl oz)", 0.0295735);
        volumeUnits.put("Cup (US)", 0.236588);
        volumeUnits.put("Pint (pt)", 0.473176);
        volumeUnits.put("Quart (qt)", 0.946353);
        volumeUnits.put("Gallon (gal)", 3.78541);

        // Speed units -> factor to meters per second
        speedUnits.put("Meter/second (m/s)", 1.0);
        speedUnits.put("Kilometer/hour (km/h)", 0.2777777778);
        speedUnits.put("Mile/hour (mph)", 0.44704);
        speedUnits.put("Knot (kt)", 0.5144444444);
        speedUnits.put("Foot/second (ft/s)", 0.3048);

        // Data Size units -> factor to byte (using 1024 for KB/MB etc)
        dataSizeUnits.put("Bit (b)", 0.125); // 1 bit = 0.125 bytes
        dataSizeUnits.put("Byte (B)", 1.0);
        dataSizeUnits.put("Kilobyte (KB)", 1024.0);
        dataSizeUnits.put("Megabyte (MB)", 1024.0 * 1024.0);
        dataSizeUnits.put("Gigabyte (GB)", 1024.0 * 1024.0 * 1024.0);
        dataSizeUnits.put("Terabyte (TB)", 1024.0 * 1024.0 * 1024.0 * 1024.0);

        // Area units -> factor to square meter
        areaUnits.put("Square millimeter (mm²)", 0.000001);
        areaUnits.put("Square centimeter (cm²)", 0.0001);
        areaUnits.put("Square meter (m²)", 1.0);
        areaUnits.put("Square kilometer (km²)", 1000000.0);
        areaUnits.put("Hectare (ha)", 10000.0);
        areaUnits.put("Acre (ac)", 4046.8564224);
        areaUnits.put("Square mile (mi²)", 2589988.110336);

        // Time units -> factor to second
        timeUnits.put("Millisecond (ms)", 0.001);
        timeUnits.put("Second (s)", 1.0);
        timeUnits.put("Minute (min)", 60.0);
        timeUnits.put("Hour (h)", 3600.0);
        timeUnits.put("Day (d)", 86400.0);
        timeUnits.put("Week (wk)", 604800.0);
        // approximate year
        timeUnits.put("Year (yr)", 31557600.0); // 365.25*86400

        // Pressure units -> factor to pascal
        pressureUnits.put("Pascal (Pa)", 1.0);
        pressureUnits.put("Kilopascal (kPa)", 1000.0);
        pressureUnits.put("Bar (bar)", 100000.0);
        pressureUnits.put("Atmosphere (atm)", 101325.0);
        pressureUnits.put("Pound/psi (psi)", 6894.75729);
        pressureUnits.put("Torr (mmHg)", 133.3223684);
    }

    private void setupCategorySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String category = (String) parent.getItemAtPosition(position);
                populateUnitSpinners(category);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // default populate
        populateUnitSpinners("Length");
    }

    private void populateUnitSpinners(String category) {
        String[] units;
        if (category.equals("Length")) {
            units = lengthUnits.keySet().toArray(new String[0]);
        } else if (category.equals("Mass")) {
            units = massUnits.keySet().toArray(new String[0]);
        } else if (category.equals("Temperature")) {
            units = temperatureUnits;
        } else if (category.equals("Volume")) {
            units = volumeUnits.keySet().toArray(new String[0]);
        } else if (category.equals("Speed")) {
            units = speedUnits.keySet().toArray(new String[0]);
        } else if (category.equals("Data Size")) {
            units = dataSizeUnits.keySet().toArray(new String[0]);
        } else if (category.equals("Area")) {
            units = areaUnits.keySet().toArray(new String[0]);
        } else if (category.equals("Time")) {
            units = timeUnits.keySet().toArray(new String[0]);
        } else if (category.equals("Pressure")) {
            units = pressureUnits.keySet().toArray(new String[0]);
        } else {
            units = new String[]{};
        }

        ArrayAdapter<String> adapterUnits = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, units);
        adapterUnits.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrom.setAdapter(adapterUnits);
        spinnerTo.setAdapter(adapterUnits);
        // select different default indices if possible
        if (units.length >= 2) {
            spinnerFrom.setSelection(0);
            spinnerTo.setSelection(1);
        }
    }

    private void onConvert() {
        String inputStr = etValue.getText().toString().trim();
        if (inputStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_value), Toast.LENGTH_SHORT).show();
            return;
        }

        double inputValue;
        try {
            inputValue = Double.parseDouble(inputStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.invalid_number), Toast.LENGTH_SHORT).show();
            return;
        }

        String category = (String) spinnerCategory.getSelectedItem();
        String fromUnit = (String) spinnerFrom.getSelectedItem();
        String toUnit = (String) spinnerTo.getSelectedItem();

        double result;
        try {
            result = convert(category, fromUnit, toUnit, inputValue);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        String inputFormatted = trimTrailingZeros(inputValue);
        String resultFormatted = trimTrailingZeros(result);

        String display = inputFormatted + " " + getShortUnit(fromUnit) + " = " + resultFormatted + " " + getShortUnit(toUnit);
        tvResult.setText(display);

        // Add to history (most recent first)
        addHistory(display);
    }

    private void onSwap() {
        int fromPos = spinnerFrom.getSelectedItemPosition();
        int toPos = spinnerTo.getSelectedItemPosition();
        spinnerFrom.setSelection(toPos >= 0 ? toPos : 0);
        spinnerTo.setSelection(fromPos >= 0 ? fromPos : 0);

        // swap input and result value to speed reverse conversion when possible
        String resultText = tvResult.getText().toString();
        if (!resultText.isEmpty() && !resultText.equals(getString(R.string.result_placeholder))) {
            // try to extract the right-hand numeric value from result string "a U = b V"
            String[] parts = resultText.split("=");
            if (parts.length == 2) {
                String right = parts[1].trim();
                // take first token as number
                String[] tokens = right.split("\\s+");
                if (tokens.length > 0) {
                    try {
                        double val = Double.parseDouble(tokens[0]);
                        etValue.setText(trimTrailingZeros(val));
                    } catch (NumberFormatException ignored) { }
                }
            }
        }
    }

    private void copyResultToClipboard() {
        String text = tvResult.getText().toString();
        if (text == null || text.isEmpty() || text.equals(getString(R.string.result_placeholder))) {
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("conversion_result", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, getString(R.string.copied_result), Toast.LENGTH_SHORT).show();
        }
    }

    private String getShortUnit(String displayName) {
        if (displayName == null) return "";
        int start = displayName.indexOf('(');
        int end = displayName.indexOf(')');
        if (start >= 0 && end > start) {
            return displayName.substring(start + 1, end);
        } else {
            return displayName;
        }
    }

    private String trimTrailingZeros(double val) {
        String formatted = String.format("%.6f", val);
        formatted = formatted.replaceAll("\\.?0+$", "");
        return formatted;
    }

    private double convert(String category, String fromUnit, String toUnit, double value) {
        if (category.equals("Length")) {
            double fromFactor = lengthUnits.get(fromUnit);
            double toFactor = lengthUnits.get(toUnit);
            return value * fromFactor / toFactor;
        } else if (category.equals("Mass")) {
            double fromFactor = massUnits.get(fromUnit);
            double toFactor = massUnits.get(toUnit);
            return value * fromFactor / toFactor;
        } else if (category.equals("Temperature")) {
            return convertTemperature(fromUnit, toUnit, value);
        } else if (category.equals("Volume")) {
            double fromFactor = volumeUnits.get(fromUnit);
            double toFactor = volumeUnits.get(toUnit);
            return value * fromFactor / toFactor;
        } else if (category.equals("Speed")) {
            double fromFactor = speedUnits.get(fromUnit);
            double toFactor = speedUnits.get(toUnit);
            return value * fromFactor / toFactor;
        } else if (category.equals("Data Size")) {
            double fromFactor = dataSizeUnits.get(fromUnit);
            double toFactor = dataSizeUnits.get(toUnit);
            return value * fromFactor / toFactor;
        } else if (category.equals("Area")) {
            double fromFactor = areaUnits.get(fromUnit);
            double toFactor = areaUnits.get(toUnit);
            return value * fromFactor / toFactor;
        } else if (category.equals("Time")) {
            double fromFactor = timeUnits.get(fromUnit);
            double toFactor = timeUnits.get(toUnit);
            return value * fromFactor / toFactor;
        } else if (category.equals("Pressure")) {
            double fromFactor = pressureUnits.get(fromUnit);
            double toFactor = pressureUnits.get(toUnit);
            return value * fromFactor / toFactor;
        } else {
            throw new IllegalArgumentException("Unsupported category");
        }
    }

    private double convertTemperature(String fromDisplay, String toDisplay, double value) {
        String from = getTemperatureKey(fromDisplay);
        String to = getTemperatureKey(toDisplay);

        // Convert from source to Celsius
        double celsius;
        switch (from) {
            case "Celsius":
                celsius = value;
                break;
            case "Fahrenheit":
                celsius = (value - 32) * 5.0 / 9.0;
                break;
            case "Kelvin":
                celsius = value - 273.15;
                break;
            default:
                throw new IllegalArgumentException("Unsupported temperature unit");
        }

        // Convert Celsius to target
        switch (to) {
            case "Celsius":
                return celsius;
            case "Fahrenheit":
                return celsius * 9.0 / 5.0 + 32;
            case "Kelvin":
                return celsius + 273.15;
            default:
                throw new IllegalArgumentException("Unsupported temperature unit");
        }
    }

    private String getTemperatureKey(String displayName) {
        if (displayName == null) return "";
        if (displayName.toLowerCase().contains("celsius")) return "Celsius";
        if (displayName.toLowerCase().contains("fahrenheit")) return "Fahrenheit";
        if (displayName.toLowerCase().contains("kelvin")) return "Kelvin";
        return displayName;
    }

    // Custom units persistence format:
    // { "Length": [ { "name": "Custom (C)", "factor": 0.5 }, ... ], "Mass": [...], ... }
    private void saveCustomUnits() {
        JSONObject root = new JSONObject();
        try {
            for (String cat : getLinearCategories()) {
                JSONArray arr = new JSONArray();
                LinkedHashMap<String, Double> map = getUnitMapForCategory(cat);
                // Skip base built-in units by including only those that were not present in defaults?
                // To keep it simple, we save the custom units only by checking a dedicated marker:
                // We'll store only those units that are not in the built-in set by keeping a prefix in prefs.
                // Simpler: Save all units — on load we'll merge but avoid duplication.
                for (Map.Entry<String, Double> e : map.entrySet()) {
                    JSONObject u = new JSONObject();
                    u.put("name", e.getKey());
                    u.put("factor", e.getValue());
                    arr.put(u);
                }
                root.put(cat, arr);
            }
        } catch (JSONException ignored) { }

        prefs.edit().putString(KEY_CUSTOM_UNITS, root.toString()).apply();
    }

    private void loadCustomUnits() {
        // Merge saved units into maps. For backward compatibility, we allow missing prefs.
        String json = prefs.getString(KEY_CUSTOM_UNITS, null);
        if (json == null) {
            return;
        }
        try {
            JSONObject root = new JSONObject(json);
            for (String cat : getLinearCategories()) {
                if (!root.has(cat)) continue;
                JSONArray arr = root.getJSONArray(cat);
                LinkedHashMap<String, Double> map = getUnitMapForCategory(cat);
                if (map == null) continue;
                map.clear(); // wipe current and re-add built-ins + saved to preserve order
                // re-initialize defaults for category then add saved entries
                reinitDefaultsForCategory(cat, map);
                // add saved ones (may duplicate, but we'll overwrite factor)
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject u = arr.getJSONObject(i);
                    String name = u.getString("name");
                    double factor = u.getDouble("factor");
                    map.put(name, factor);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void reinitDefaultsForCategory(String category, LinkedHashMap<String, Double> map) {
        // re-add built-in defaults for this category
        if (category.equals("Length")) {
            map.put("Millimeter (mm)", 0.001);
            map.put("Centimeter (cm)", 0.01);
            map.put("Meter (m)", 1.0);
            map.put("Kilometer (km)", 1000.0);
            map.put("Inch (in)", 0.0254);
            map.put("Foot (ft)", 0.3048);
            map.put("Yard (yd)", 0.9144);
            map.put("Mile (mi)", 1609.34);
        } else if (category.equals("Mass")) {
            map.put("Milligram (mg)", 0.000001);
            map.put("Gram (g)", 0.001);
            map.put("Kilogram (kg)", 1.0);
            map.put("Tonne (t)", 1000.0);
            map.put("Ounce (oz)", 0.0283495);
            map.put("Pound (lb)", 0.453592);
        } else if (category.equals("Volume")) {
            map.put("Milliliter (mL)", 0.001);
            map.put("Liter (L)", 1.0);
            map.put("Cubic meter (m³)", 1000.0);
            map.put("Teaspoon (tsp)", 0.00492892);
            map.put("Tablespoon (tbsp)", 0.0147868);
            map.put("Fluid ounce (fl oz)", 0.0295735);
            map.put("Cup (US)", 0.236588);
            map.put("Pint (pt)", 0.473176);
            map.put("Quart (qt)", 0.946353);
            map.put("Gallon (gal)", 3.78541);
        } else if (category.equals("Speed")) {
            map.put("Meter/second (m/s)", 1.0);
            map.put("Kilometer/hour (km/h)", 0.2777777778);
            map.put("Mile/hour (mph)", 0.44704);
            map.put("Knot (kt)", 0.5144444444);
            map.put("Foot/second (ft/s)", 0.3048);
        } else if (category.equals("Data Size")) {
            map.put("Bit (b)", 0.125);
            map.put("Byte (B)", 1.0);
            map.put("Kilobyte (KB)", 1024.0);
            map.put("Megabyte (MB)", 1024.0 * 1024.0);
            map.put("Gigabyte (GB)", 1024.0 * 1024.0 * 1024.0);
            map.put("Terabyte (TB)", 1024.0 * 1024.0 * 1024.0 * 1024.0);
        } else if (category.equals("Area")) {
            map.put("Square millimeter (mm²)", 0.000001);
            map.put("Square centimeter (cm²)", 0.0001);
            map.put("Square meter (m²)", 1.0);
            map.put("Square kilometer (km²)", 1_000_000.0);
            map.put("Hectare (ha)", 10000.0);
            map.put("Acre (ac)", 4046.8564224);
            map.put("Square mile (mi²)", 2589988.110336);
        } else if (category.equals("Time")) {
            map.put("Millisecond (ms)", 0.001);
            map.put("Second (s)", 1.0);
            map.put("Minute (min)", 60.0);
            map.put("Hour (h)", 3600.0);
            map.put("Day (d)", 86400.0);
            map.put("Week (wk)", 604800.0);
            map.put("Year (yr)", 31557600.0);
        } else if (category.equals("Pressure")) {
            map.put("Pascal (Pa)", 1.0);
            map.put("Kilopascal (kPa)", 1000.0);
            map.put("Bar (bar)", 100000.0);
            map.put("Atmosphere (atm)", 101325.0);
            map.put("Pound/psi (psi)", 6894.75729);
            map.put("Torr (mmHg)", 133.3223684);
        }
    }

    private String[] getLinearCategories() {
        return new String[] { "Length", "Mass", "Volume", "Speed", "Data Size", "Area", "Time", "Pressure" };
    }

    private LinkedHashMap<String, Double> getUnitMapForCategory(String category) {
        switch (category) {
            case "Length": return lengthUnits;
            case "Mass": return massUnits;
            case "Volume": return volumeUnits;
            case "Speed": return speedUnits;
            case "Data Size": return dataSizeUnits;
            case "Area": return areaUnits;
            case "Time": return timeUnits;
            case "Pressure": return pressureUnits;
            default: return null;
        }
    }

    private void showAddUnitDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_add_unit, null);
        spinnerDialogCategory = view.findViewById(R.id.spinnerDialogCategory);
        etUnitName = view.findViewById(R.id.etUnitName);
        etUnitFactor = view.findViewById(R.id.etUnitFactor);

        // Only allow linear categories
        String[] cats = getLinearCategories();
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cats);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDialogCategory.setAdapter(catAdapter);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.add_unit))
                .setView(view)
                .setPositiveButton(getString(R.string.add_unit), (dialog, which) -> {
                    String category = (String) spinnerDialogCategory.getSelectedItem();
                    String name = etUnitName.getText().toString().trim();
                    String factorStr = etUnitFactor.getText().toString().trim();
                    if (name.isEmpty() || factorStr.isEmpty()) {
                        Toast.makeText(this, "Name and factor required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double factor;
                    try {
                        factor = Double.parseDouble(factorStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid factor", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    LinkedHashMap<String, Double> map = getUnitMapForCategory(category);
                    if (map == null) return;
                    map.put(name, factor);
                    saveCustomUnits();
                    // If current category matches, refresh spinners
                    String current = (String) spinnerCategory.getSelectedItem();
                    if (current.equals(category)) {
                        populateUnitSpinners(category);
                    }
                    Toast.makeText(this, getString(R.string.unit_added), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.close), null)
                .show();
    }

    private void addHistory(String entry) {
        // push to top
        history.add(0, entry);
        // limit to 100 entries
        if (history.size() > 100) history.remove(history.size() - 1);
        saveHistory();
    }

    private void saveHistory() {
        JSONArray arr = new JSONArray();
        for (String s : history) arr.put(s);
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply();
    }

    private void loadHistory() {
        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            history.clear();
            for (int i = 0; i < arr.length(); i++) {
                history.add(arr.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showHistoryDialog() {
        if (history.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.history))
                    .setMessage(getString(R.string.empty_history))
                    .setPositiveButton(getString(R.string.close), null)
                    .show();
            return;
        }
        ListView lv = new ListView(this);
        ListAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, history);
        lv.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.history))
                .setView(lv)
                .setPositiveButton(getString(R.string.close), null)
                .setNeutralButton(getString(R.string.clear_history), (d, w) -> {
                    history.clear();
                    saveHistory();
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
                })
                .create();

        lv.setOnItemClickListener((parent, view, position, id) -> {
            // copy selected history item to clipboard
            String item = history.get(position);
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("history_item", item);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, getString(R.string.copied_result), Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
