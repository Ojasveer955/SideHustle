package com.example.sidehustle;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PersonalDetailsFragment extends Fragment {

    private static final String TAG = "PersonalDetailsFragment";
    
    // Change tvName to tvUserName to match the ID in the XML
    private TextView tvUserName, tvUserEmail, tvUserPhone;
    private Button btnEditPersonal;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal_details, container, false);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        // Initialize views with correct IDs
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        tvUserPhone = view.findViewById(R.id.tv_user_phone);
        btnEditPersonal = view.findViewById(R.id.btn_edit_personal);
        
        // Setup button listeners
        btnEditPersonal.setOnClickListener(v -> showEditDialog());
        
        // Load user data
        loadUserData();
        
        return view;
    }
    
    private void loadUserData() {
        if (currentUser == null) return;
        
        // Set email from Firebase Auth
        String email = currentUser.getEmail();
        if (email != null && !email.isEmpty()) {
            tvUserEmail.setText(email);
        }
        
        // First check if there's a name in the ProfileActivity
        if (getActivity() instanceof ProfileActivity) {
            ProfileActivity activity = (ProfileActivity) getActivity();
            String headerName = activity.getHeaderName();
            
            if (headerName != null && !headerName.isEmpty()) {
                tvUserName.setText(headerName);
            }
        }
        
        // Get additional data from Firestore
        db.collection("Users")
            .document(currentUser.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Map<String, Object> personalData = (Map<String, Object>) documentSnapshot.get("Personal");
                    if (personalData != null) {
                        // Update UI with user data
                        String name = (String) personalData.get("name");
                        String userEmail = (String) personalData.get("email");
                        Object phoneObj = personalData.get("phone");
                        String phone = phoneObj != null ? phoneObj.toString() : "";
                        
                        if (name != null && !name.isEmpty()) {
                            tvUserName.setText(name);
                            
                            // Sync with header name
                            if (getActivity() instanceof ProfileActivity) {
                                ((ProfileActivity) getActivity()).updateHeaderName(name);
                            }
                        } else {
                            // If no name in Firestore, use display name from Firebase Auth
                            String displayName = currentUser.getDisplayName();
                            if (displayName != null && !displayName.isEmpty()) {
                                tvUserName.setText(displayName);
                                
                                // Sync with header name
                                if (getActivity() instanceof ProfileActivity) {
                                    ((ProfileActivity) getActivity()).updateHeaderName(displayName);
                                }
                            }
                        }
                        
                        if (userEmail != null && !userEmail.isEmpty()) {
                            tvUserEmail.setText(userEmail);
                        }
                        
                        if (phone != null && !phone.isEmpty()) {
                            tvUserPhone.setText(phone);
                        } else {
                            tvUserPhone.setText(getString(R.string.not_provided));
                        }
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading user data", e);
                Toast.makeText(getContext(), 
                        "Error loading profile data", Toast.LENGTH_SHORT).show();
            });
    }

    private void showEditDialog() {
        // Inflate the dialog layout
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_personal, null);
        
        // Find the input fields and layouts
        TextInputEditText emailInput = dialogView.findViewById(R.id.et_email);
        TextInputEditText phoneInput = dialogView.findViewById(R.id.et_phone);
        TextInputLayout emailLayout = dialogView.findViewById(R.id.til_email);
        
        // Pre-fill with current values
        emailInput.setText(tvUserEmail.getText().toString());
        phoneInput.setText(tvUserPhone.getText().toString().equals(getString(R.string.not_provided)) ? 
                "" : tvUserPhone.getText().toString());
        
        // Create the dialog with null click listeners (we'll set them manually)
        AlertDialog dialog = builder.setTitle("Edit Contact Information")
                .setView(dialogView)
                .setPositiveButton("Save", null) // We'll override this below
                .setNegativeButton("Cancel", null)
                .create();
        
        // Show the dialog
        dialog.show();
        
        // Now override the positive button to perform validation
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Get input values
            String email = emailInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            boolean isValid = true;
            
            // Email validation - only validate format if email is provided
            if (email.isEmpty()) {
                emailLayout.setError("Email cannot be empty");
                isValid = false;
            } else if (!isValidEmail(email)) {
                emailLayout.setError("Please enter a valid email address");
                isValid = false;
            } else {
                emailLayout.setError(null);
            }
            
            // No need for phone validation since we're using maxLength
            
            // If all validations pass, update data and dismiss dialog
            if (isValid) {
                Log.d(TAG, "Validation passed, updating data");
                // Get the current name - we're not changing it
                String name = tvUserName.getText().toString();
                updatePersonalData(name, email, phone);
                dialog.dismiss();
            } else {
                Log.d(TAG, "Validation failed");
            }
        });
    }

    /**
     * Validates if the provided string is a valid email address
     * @param email The email address to validate
     * @return True if the email is valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void updatePersonalData(String name, String email, String phone) {
        if (currentUser == null) return;
        
        // Create personal data map
        Map<String, Object> personal = new HashMap<>();
        personal.put("name", name);
        personal.put("email", email);
        personal.put("phone", phone);
        
        // Update in Firestore
        db.collection("Users")
                .document(currentUser.getUid())
                .update("Personal", personal)
                .addOnSuccessListener(aVoid -> {
                    // Update UI
                    tvUserName.setText(name);
                    tvUserEmail.setText(email);
                    tvUserPhone.setText(phone.isEmpty() ? getString(R.string.not_provided) : phone);
                    
                    // Always update the ProfileActivity name to keep in sync
                    if (getActivity() instanceof ProfileActivity) {
                        ((ProfileActivity) getActivity()).updateHeaderName(name);
                    }
                    
                    Toast.makeText(getContext(), "Contact information updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // If document doesn't exist yet, create it
                    db.collection("Users")
                            .document(currentUser.getUid())
                            .set(new HashMap<String, Object>() {{
                                put("Personal", personal);
                            }})
                            .addOnSuccessListener(aVoid -> {
                                // Update UI
                                tvUserName.setText(name);
                                tvUserEmail.setText(email);
                                tvUserPhone.setText(phone.isEmpty() ? getString(R.string.not_provided) : phone);
                                
                                // Update the ProfileActivity name as well
                                if (getActivity() instanceof ProfileActivity) {
                                    ((ProfileActivity) getActivity()).updateHeaderName(name);
                                }
                                
                                Toast.makeText(getContext(), "Profile created successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(ex -> {
                                Log.e(TAG, "Error creating user document", ex);
                                Toast.makeText(getContext(), 
                                        "Error updating details: " + ex.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            });
                });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // When fragment becomes visible, ensure names are in sync
        if (getActivity() instanceof ProfileActivity && tvUserName != null) {
            ProfileActivity activity = (ProfileActivity) getActivity();
            String headerName = activity.getHeaderName();
            
            // If header name exists and personal name doesn't match, update personal name
            if (headerName != null && !headerName.isEmpty() && 
                (tvUserName.getText() == null || !tvUserName.getText().toString().equals(headerName))) {
                tvUserName.setText(headerName);
            }
            // If personal name exists and header name doesn't match, update header name
            else if (tvUserName.getText() != null && !tvUserName.getText().toString().isEmpty()) {
                activity.updateHeaderName(tvUserName.getText().toString());
            }
        }
    }
    
    // Removed all image-related methods:
    // - showImageSourceDialog()
    // - checkCameraPermission()
    // - checkStoragePermission()
    // - openCamera()
    // - openGallery()
    // - onRequestPermissionsResult()
    // - onActivityResult()
    // - uploadImageToFirebase()
    // - updateUserProfilePicture()
}