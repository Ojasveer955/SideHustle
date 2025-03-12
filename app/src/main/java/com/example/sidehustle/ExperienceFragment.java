package com.example.sidehustle;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExperienceFragment extends Fragment {

    private static final String TAG = "ExperienceFragment";
    private LinearLayout experienceContainer;
    private TextView tvNoExperience;
    private Button btnAddExperience;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private List<Map<String, Object>> experienceList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_experience, container, false);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        // Initialize views
        experienceContainer = view.findViewById(R.id.experience_container);
        tvNoExperience = view.findViewById(R.id.tv_no_experience);
        btnAddExperience = view.findViewById(R.id.btn_add_experience);
        
        // Setup button listener
        btnAddExperience.setOnClickListener(v -> showAddExperienceDialog());
        
        // Load experience data
        loadExperiences();
        
        return view;
    }
    
    private void loadExperiences() {
        if (currentUser == null) return;
        
        experienceList.clear();
        
        db.collection("Users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            // First try to get as a list (new format)
                            List<Map<String, Object>> experiences = 
                                (List<Map<String, Object>>) documentSnapshot.get("Experience");
                            
                            if (experiences != null && !experiences.isEmpty()) {
                                // Process each experience in the array
                                for (Map<String, Object> exp : experiences) {
                                    experienceList.add(new HashMap<>(exp));
                                }
                                displayExperience();
                                return; // Exit early if we successfully processed as a list
                            }
                            
                            // If we get here, either the list was null or empty
                            // Try to get as a map (old format)
                            Map<String, Object> experienceMap = 
                                (Map<String, Object>) documentSnapshot.get("Experience");
                                
                            if (experienceMap != null && !experienceMap.isEmpty()) {
                                // Convert the map to a list item
                                experienceList.add(new HashMap<>(experienceMap));
                                displayExperience();
                                return;
                            }
                            
                            // If we get here, no experiences were found in any format
                            showNoExperienceMessage();
                            
                        } catch (ClassCastException e) {
                            // If we get a class cast exception, the structure might be different
                            Log.w(TAG, "Different experience data structure detected", e);
                            try {
                                // Try to get as a map instead
                                Map<String, Object> experienceMap = 
                                    (Map<String, Object>) documentSnapshot.get("Experience");
                                    
                                if (experienceMap != null && !experienceMap.isEmpty()) {
                                    // Convert the map to a list item
                                    experienceList.add(new HashMap<>(experienceMap));
                                    displayExperience();
                                    return;
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "Error parsing experience data", ex);
                            }
                            showNoExperienceMessage();
                        }
                    } else {
                        showNoExperienceMessage();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading experiences", e);
                    Toast.makeText(getContext(), "Error loading experiences: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    showNoExperienceMessage();
                });
    }
    
    private void displayExperience() {
        experienceContainer.removeAllViews();
        
        if (experienceList.isEmpty()) {
            showNoExperienceMessage();
            return;
        }
        
        tvNoExperience.setVisibility(View.GONE);
        
        for (Map<String, Object> exp : experienceList) {
            View expView = getLayoutInflater().inflate(R.layout.item_experience, null);
            
            TextView tvCompany = expView.findViewById(R.id.tv_company);
            TextView tvRole = expView.findViewById(R.id.tv_role);
            TextView tvDateRange = expView.findViewById(R.id.tv_date_range);
            TextView tvDuration = expView.findViewById(R.id.tv_duration);
            Button btnDelete = expView.findViewById(R.id.btn_delete_experience);
            
            // Set values using the correct field names
            String company = String.valueOf(exp.get("Company"));  // Use "Company" instead of "company"
            String role = String.valueOf(exp.get("Role"));        // Use "Role" instead of "title"
            tvCompany.setText(company);
            tvRole.setText(role);
            
            // Set date range if available
            String startDate = (String) exp.get("startDate");
            String endDate = (String) exp.get("endDate");
            if (startDate != null && endDate != null) {
                String dateRange = startDate + " - " + endDate;
                tvDateRange.setText(dateRange);
            } else {
                tvDateRange.setVisibility(View.GONE);  // Hide if dates not available
            }
            
            // Set duration from Years field
            Object yearsObj = exp.get("Years");
            if (yearsObj != null) {
                double years = yearsObj instanceof Number ? ((Number) yearsObj).doubleValue() : 0;
                String yearsText;
                
                if (years < 0.5) {
                    yearsText = "< 1 year";
                } else {
                    // Round to nearest year
                    int roundedYears = (int) Math.round(years);
                    yearsText = roundedYears + (roundedYears == 1 ? " year" : " years");
                }
                tvDuration.setText(yearsText);
            } else {
                tvDuration.setVisibility(View.GONE);  // Hide if years not available
            }
            
            btnDelete.setOnClickListener(v -> {
                showDeleteConfirmationDialog(exp);
            });
            
            experienceContainer.addView(expView);
        }
    }
    
    private void showNoExperienceMessage() {
        experienceContainer.removeAllViews();
        tvNoExperience.setVisibility(View.VISIBLE);
    }
    
    private void showAddExperienceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_experience, null);
        
        TextInputEditText companyInput = dialogView.findViewById(R.id.et_company);
        TextInputEditText roleInput = dialogView.findViewById(R.id.et_role);
        TextInputEditText startDateInput = dialogView.findViewById(R.id.et_start_date);
        TextInputEditText endDateInput = dialogView.findViewById(R.id.et_end_date);
        CheckBox checkboxPresent = dialogView.findViewById(R.id.checkbox_present);
        
        // Set up date pickers
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        
        // Start date picker
        startDateInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        String date = (month + 1) + "/" + year;
                        startDateInput.setText(date);
                    },
                    currentYear, currentMonth, 1);
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            
            // Fix for hiding day spinner - use try/catch to handle potential errors
            try {
                // Hide day spinner
                View daySpinner = datePickerDialog.getDatePicker().findViewById(
                        Resources.getSystem().getIdentifier("day", "id", "android"));
                if (daySpinner != null) {
                    daySpinner.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not hide day spinner: " + e.getMessage());
            }
            
            datePickerDialog.show();
        });
        
        // End date picker
        endDateInput.setOnClickListener(v -> {
            if (!checkboxPresent.isChecked()) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        requireContext(),
                        (view, year, month, dayOfMonth) -> {
                            String date = (month + 1) + "/" + year;
                            endDateInput.setText(date);
                        },
                        currentYear, currentMonth, 1);
                datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
                
                // Fix for hiding day spinner - use try/catch to handle potential errors
                try {
                    // Hide day spinner
                    View daySpinner = datePickerDialog.getDatePicker().findViewById(
                            Resources.getSystem().getIdentifier("day", "id", "android"));
                    if (daySpinner != null) {
                        daySpinner.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not hide day spinner: " + e.getMessage());
                }
                
                datePickerDialog.show();
            }
        });
        
        // Handle "Present" checkbox
        checkboxPresent.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                endDateInput.setText("Present");
                endDateInput.setEnabled(false);
            } else {
                endDateInput.setText("");
                endDateInput.setEnabled(true);
            }
        });
        
        AlertDialog dialog = builder.setTitle("Add Experience")
                .setView(dialogView)
                .setPositiveButton("Save", null) // We'll set listener later to prevent auto-dismiss
                .setNegativeButton("Cancel", null)
                .create();
        
        dialog.show();
        
        // Set positive button click listener
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String company = companyInput.getText().toString().trim();
            String role = roleInput.getText().toString().trim();
            String startDate = startDateInput.getText().toString().trim();
            String endDate = endDateInput.getText().toString().trim();
            
            // Validation
            if (company.isEmpty() || role.isEmpty() || startDate.isEmpty() || endDate.isEmpty()) {
                Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Calculate years
            double years = calculateYearsBetweenDates(startDate, endDate);
            
            // Add experience
            addNewExperience(role, company, startDate, endDate, years);
            dialog.dismiss();
        });
    }
    
    private double calculateYearsBetweenDates(String startDateStr, String endDateStr) {
        try {
            // Parse dates (assuming format MM/YYYY)
            String[] startParts = startDateStr.split("/");
            int startMonth = Integer.parseInt(startParts[0]);
            int startYear = Integer.parseInt(startParts[1]);
            
            int endMonth, endYear;
            if (endDateStr.equals("Present")) {
                // Use current date for "Present"
                Calendar cal = Calendar.getInstance();
                endMonth = cal.get(Calendar.MONTH) + 1; // Calendar months are 0-based
                endYear = cal.get(Calendar.YEAR);
            } else {
                String[] endParts = endDateStr.split("/");
                endMonth = Integer.parseInt(endParts[0]);
                endYear = Integer.parseInt(endParts[1]);
            }
            
            // Calculate difference
            int years = endYear - startYear;
            int months = endMonth - startMonth;
            
            // Adjust for negative months
            if (months < 0) {
                years--;
                months += 12;
            }
            
            // Return as decimal (years + months/12)
            return years + (months / 12.0);
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating years", e);
            return 0;
        }
    }
    
    private void showDeleteConfirmationDialog(Map<String, Object> experience) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Experience")
                .setMessage("Are you sure you want to delete this experience?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteExperience(experience);
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
    
    private void addNewExperience(String title, String company, String startDate, String endDate, double years) {
        if (currentUser == null) return;
        
        // Create a document reference
        DocumentReference userRef = db.collection("Users").document(currentUser.getUid());
        
        // Create a map for the new experience using the existing field names
        Map<String, Object> experienceData = new HashMap<>();
        experienceData.put("Role", title);        // Use "Role" instead of "title"
        experienceData.put("Company", company);   // Use "Company" instead of "company"
        experienceData.put("Years", years);       // Just store the years as before
        
        // Store these for display purposes but they aren't part of your main structure
        experienceData.put("startDate", startDate);
        experienceData.put("endDate", endDate);
        
        // First check if the user document exists
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Check if the Experience array already exists
                if (documentSnapshot.contains("Experience")) {
                    // Use FieldValue.arrayUnion to add to the array without overwriting
                    userRef.update("Experience", FieldValue.arrayUnion(experienceData))
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Experience added successfully", Toast.LENGTH_SHORT).show();
                            loadExperiences(); // Refresh the list
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Error adding experience: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                        });
                } else {
                    // Experience field doesn't exist, create it
                    List<Map<String, Object>> experiences = new ArrayList<>();
                    experiences.add(experienceData);
                    
                    userRef.update("Experience", experiences)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Experience added successfully", Toast.LENGTH_SHORT).show();
                            loadExperiences(); // Refresh the list
                        })
                        .addOnFailureListener(e -> {
                            // If update fails, try set with merge option
                            Map<String, Object> data = new HashMap<>();
                            data.put("Experience", experiences);
                            
                            userRef.set(data, SetOptions.merge())
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(getContext(), "Experience added successfully", Toast.LENGTH_SHORT).show();
                                    loadExperiences(); // Refresh the list
                                })
                                .addOnFailureListener(e2 -> {
                                    Toast.makeText(getContext(), "Error adding experience: " + e2.getMessage(), 
                                            Toast.LENGTH_SHORT).show();
                                });
                        });
                }
            } else {
                // Document doesn't exist, create it with the experience
                Map<String, Object> userData = new HashMap<>();
                List<Map<String, Object>> experiences = new ArrayList<>();
                experiences.add(experienceData);
                userData.put("Experience", experiences); // Use "Experience" not "Experiences"
                
                userRef.set(userData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Experience added successfully", Toast.LENGTH_SHORT).show();
                        loadExperiences(); // Refresh the list
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error adding experience: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    });
            }
        });
    }
    
    private void deleteExperience(Map<String, Object> experience) {
        if (currentUser == null) return;
        
        // Use FieldValue.arrayRemove to remove the specific item from the array
        DocumentReference userRef = db.collection("Users").document(currentUser.getUid());
        userRef.update("Experience", FieldValue.arrayRemove(experience))  // Use "Experience" not "Experiences"
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Experience deleted", 
                        Toast.LENGTH_SHORT).show();
                // Refresh the list from Firestore
                loadExperiences();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting experience", e);
                Toast.makeText(getContext(), "Error deleting experience: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            });
    }
}