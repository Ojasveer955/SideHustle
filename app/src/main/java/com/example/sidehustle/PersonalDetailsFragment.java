package com.example.sidehustle;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions; // <-- Import added
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions; // <-- Import added for SetOptions.merge()

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PersonalDetailsFragment extends Fragment {

    private static final String TAG = "PersonalDetailsFragment";

    private TextView tvUserName, tvUserEmail, tvUserPhone;
    private Button btnEditPersonal;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private ImageView ivProfilePic;
    private Button btnChangePic;
    private Uri cameraImageUri;
    private int pendingAction = -1; // 0 = camera, 1 = gallery

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal_details, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        tvUserPhone = view.findViewById(R.id.tv_user_phone);
        btnEditPersonal = view.findViewById(R.id.btn_edit_personal);
        ivProfilePic = view.findViewById(R.id.iv_profile_pic);
        btnChangePic = view.findViewById(R.id.btn_change_pic);

        btnChangePic.setOnClickListener(v -> showImageSourceDialog());

        loadProfilePic();

        btnEditPersonal.setOnClickListener(v -> showEditDialog());

        loadUserData();

        // --- Register Activity Result Launchers ---

        Log.d(TAG, "Registering Permission Launcher");
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissionsResult -> {
                    Log.d(TAG, "Permission Launcher Result Received. pendingAction: " + pendingAction);
                    permissionsResult.forEach((permission, isGranted) -> {
                        Log.d(TAG, "System Result -> Permission: " + permission + ", Granted: " + isGranted);
                    });

                    boolean cameraGranted = permissionsResult.getOrDefault(Manifest.permission.CAMERA, false);

                    // Determine which storage permission to check based on SDK version
                    String storagePermissionToCheck;
                    boolean storageGrantedResult;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        storagePermissionToCheck = Manifest.permission.READ_MEDIA_IMAGES;
                        storageGrantedResult = permissionsResult.getOrDefault(storagePermissionToCheck, false);
                        Log.d(TAG, "Checking for READ_MEDIA_IMAGES result: " + storageGrantedResult);
                    } else {
                        storagePermissionToCheck = Manifest.permission.READ_EXTERNAL_STORAGE;
                        storageGrantedResult = permissionsResult.getOrDefault(storagePermissionToCheck, false);
                        Log.d(TAG, "Checking for READ_EXTERNAL_STORAGE result: " + storageGrantedResult);
                    }

                    boolean canProceed = false;
                    if (pendingAction == 0) { // Camera requested
                        Log.d(TAG, "Checking permissions for Camera action...");
                        if (cameraGranted) {
                            Log.d(TAG, "CAMERA permission is GRANTED for Camera action.");
                            canProceed = true;
                        } else {
                            Log.w(TAG, "CAMERA permission was DENIED for Camera action.");
                        }
                    } else if (pendingAction == 1) { // Gallery requested
                        Log.d(TAG, "Checking permissions for Gallery action (Permission: " + storagePermissionToCheck + ")...");
                        if (storageGrantedResult) { // Check the dynamically determined storage permission
                            Log.d(TAG, storagePermissionToCheck + " permission is GRANTED for Gallery action.");
                            canProceed = true;
                        } else {
                            Log.w(TAG, storagePermissionToCheck + " permission was DENIED for Gallery action.");
                        }
                    } else {
                        Log.e(TAG, "Permission result received but pendingAction is invalid: " + pendingAction);
                    }

                    // ... (rest of the callback remains the same - proceeding or showing toast) ...
                     if (canProceed) {
                        Log.d(TAG, "Required permission(s) granted, proceeding with action: " + pendingAction);
                        if (pendingAction == 0) {
                            Log.d(TAG, "Calling openCamera()...");
                            openCamera();
                        } else if (pendingAction == 1) {
                            Log.d(TAG, "Calling openGallery()...");
                            openGallery();
                        }
                    } else {
                        if (pendingAction != -1) {
                            Log.w(TAG, "Required permission(s) were DENIED for pendingAction: " + pendingAction);
                            Toast.makeText(getContext(), "Permissions required", Toast.LENGTH_SHORT).show();
                        }
                    }
                    pendingAction = -1;
                });

        Log.d(TAG, "Registering Camera Launcher");
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Camera result received: " + result.getResultCode());
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (cameraImageUri != null) {
                            Log.d(TAG, "Camera success, URI: " + cameraImageUri);
                            // Reload the image using Glide to ensure cache is handled correctly
                            loadProfilePic();
                            // Notify activity to update its image as well
                            if (getActivity() instanceof ProfileActivity) {
                                ((ProfileActivity) getActivity()).updateProfilePictureFromLocal();
                            }
                        } else {
                            Log.e(TAG, "Camera success but cameraImageUri is null");
                        }
                    } else {
                        Log.w(TAG, "Camera cancelled or failed.");
                    }
                });

        Log.d(TAG, "Registering Gallery Launcher");
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Gallery result received: " + result.getResultCode());
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null
                            && result.getData().getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        Log.d(TAG, "Gallery success, URI: " + selectedImageUri);
                        saveImageToLocal(selectedImageUri); // Save the selected image locally
                        // Reload the image using Glide to ensure cache is handled correctly
                        loadProfilePic(); // Updates fragment's image

                        // Notify activity to update its image as well
                        Log.d(TAG, "Attempting to notify ProfileActivity to update profile picture..."); // Add log
                        if (getActivity() instanceof ProfileActivity) {
                            ((ProfileActivity) getActivity()).updateProfilePictureFromLocal();
                            Log.d(TAG, "Called updateProfilePictureFromLocal() on ProfileActivity."); // Add log
                        } else {
                            Log.w(TAG, "Could not notify ProfileActivity. getActivity() is null or not ProfileActivity."); // Add log
                        }
                    } else {
                        Log.w(TAG, "Gallery cancelled or failed or no data.");
                    }
                });

        return view;
    }

    private void loadUserData() {
        if (currentUser == null) {
            Log.w(TAG, "loadUserData: currentUser is null");
            // Set defaults or show error state
            tvUserName.setText(getString(R.string.not_available));
            tvUserEmail.setText(getString(R.string.not_available));
            tvUserPhone.setText(getString(R.string.not_provided));
            return;
        }

        String email = currentUser.getEmail();
        if (email != null && !email.isEmpty()) {
            tvUserEmail.setText(email);
        } else {
            tvUserEmail.setText(getString(R.string.not_available)); // Indicate if email is missing from auth
        }

        // Try to get name from Activity first (might be more up-to-date if edited)
        if (getActivity() instanceof ProfileActivity) {
            ProfileActivity activity = (ProfileActivity) getActivity();
            String headerName = activity.getHeaderName();
            if (headerName != null && !headerName.isEmpty()) {
                tvUserName.setText(headerName);
            }
        }

        // Load data from Firestore
        db.collection("Users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> personalData = (Map<String, Object>) documentSnapshot.get("Personal");
                        if (personalData != null) {
                            String name = (String) personalData.get("name");
                            String userEmail = (String) personalData.get("email"); // Email from Firestore might be different
                            Object phoneObj = personalData.get("phone");
                            String phone = phoneObj != null ? phoneObj.toString() : "";

                            // Update name if Firestore has it and it's different or wasn't set from activity
                            if (name != null && !name.isEmpty() && (tvUserName.getText() == null || tvUserName.getText().toString().isEmpty() || !tvUserName.getText().toString().equals(name))) {
                                tvUserName.setText(name);
                                if (getActivity() instanceof ProfileActivity) {
                                    ((ProfileActivity) getActivity()).updateHeaderName(name);
                                }
                            } else if (tvUserName.getText() == null || tvUserName.getText().toString().isEmpty() || tvUserName.getText().toString().equals(getString(R.string.not_available))) {
                                // Fallback to Firebase Auth display name if Firestore name is missing or UI shows "N/A"
                                String displayName = currentUser.getDisplayName();
                                if (displayName != null && !displayName.isEmpty()) {
                                    tvUserName.setText(displayName);
                                    if (getActivity() instanceof ProfileActivity) {
                                        ((ProfileActivity) getActivity()).updateHeaderName(displayName);
                                    }
                                } else {
                                     tvUserName.setText(getString(R.string.not_available)); // Indicate if name is missing everywhere
                                }
                            }

                            // Update email if Firestore has it and it's different from Auth email
                            if (userEmail != null && !userEmail.isEmpty() && !userEmail.equals(tvUserEmail.getText().toString())) {
                                tvUserEmail.setText(userEmail);
                            }

                            // Update phone
                            if (phone != null && !phone.isEmpty()) {
                                tvUserPhone.setText(phone);
                            } else {
                                tvUserPhone.setText(getString(R.string.not_provided));
                            }
                        } else {
                             Log.w(TAG, "Firestore document exists but 'Personal' map is null");
                             // Handle case where 'Personal' field might be missing, maybe set defaults
                             if (tvUserName.getText() == null || tvUserName.getText().toString().isEmpty() || tvUserName.getText().toString().equals(getString(R.string.not_available))) {
                                 String displayName = currentUser.getDisplayName();
                                 if (displayName != null && !displayName.isEmpty()) tvUserName.setText(displayName);
                                 else tvUserName.setText(getString(R.string.not_available));
                             }
                             tvUserPhone.setText(getString(R.string.not_provided));
                        }
                    } else {
                         Log.w(TAG, "Firestore document does not exist for user: " + currentUser.getUid());
                         // Handle case where document doesn't exist, maybe set defaults from Auth
                         if (tvUserName.getText() == null || tvUserName.getText().toString().isEmpty() || tvUserName.getText().toString().equals(getString(R.string.not_available))) {
                             String displayName = currentUser.getDisplayName();
                             if (displayName != null && !displayName.isEmpty()) tvUserName.setText(displayName);
                             else tvUserName.setText(getString(R.string.not_available));
                         }
                         tvUserPhone.setText(getString(R.string.not_provided));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data from Firestore", e);
                    Toast.makeText(getContext(), "Error loading profile data", Toast.LENGTH_SHORT).show();
                    // Set defaults even on failure
                    if (tvUserName.getText() == null || tvUserName.getText().toString().isEmpty() || tvUserName.getText().toString().equals(getString(R.string.not_available))) {
                        String displayName = currentUser.getDisplayName();
                        if (displayName != null && !displayName.isEmpty()) tvUserName.setText(displayName);
                        else tvUserName.setText(getString(R.string.not_available));
                    }
                    tvUserPhone.setText(getString(R.string.not_provided));
                });
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_personal, null);

        TextInputEditText emailInput = dialogView.findViewById(R.id.et_email);
        TextInputEditText phoneInput = dialogView.findViewById(R.id.et_phone);
        TextInputLayout emailLayout = dialogView.findViewById(R.id.til_email);

        // Pre-fill with current values, handling "N/A" or "Not Provided"
        String currentEmail = tvUserEmail.getText().toString();
        String currentPhone = tvUserPhone.getText().toString();

        emailInput.setText(currentEmail.equals(getString(R.string.not_available)) ? "" : currentEmail);
        phoneInput.setText(currentPhone.equals(getString(R.string.not_provided)) ? "" : currentPhone);

        AlertDialog dialog = builder.setTitle("Edit Contact Information")
                .setView(dialogView)
                .setPositiveButton("Save", null) // Set listener later to prevent auto-dismiss
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        // Override positive button click to handle validation
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            boolean isValid = true;

            if (email.isEmpty()) {
                emailLayout.setError("Email cannot be empty");
                isValid = false;
            } else if (!isValidEmail(email)) {
                emailLayout.setError("Please enter a valid email address");
                isValid = false;
            } else {
                emailLayout.setError(null); // Clear error if valid
            }

            // Optional: Add phone validation if needed
            // TextInputLayout phoneLayout = dialogView.findViewById(R.id.til_phone);
            // if (phone.isEmpty()) { ... } else if (!isValidPhone(phone)) { ... }

            if (isValid) {
                String name = tvUserName.getText().toString();
                // Handle case where name might be "N/A" - maybe prompt user or use a default?
                if (name.equals(getString(R.string.not_available))) {
                    // Decide how to handle this - perhaps use email prefix or a placeholder?
                    // For now, let's prevent saving if name is invalid, or use a default.
                    // Option 1: Show error (requires adding name field to dialog)
                    // Option 2: Use a default (less ideal)
                    // Option 3: Allow saving (might result in "N/A" in Firestore)
                    Log.w(TAG, "Attempting to save with name as 'Not Available'");
                    // Let's allow saving for now, but this might need refinement.
                }
                updatePersonalData(name, email, phone);
                dialog.dismiss(); // Dismiss only if valid
            }
        });
    }

    private boolean isValidEmail(String email) {
        // Use Android's built-in pattern checker
        return email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void updatePersonalData(String name, String email, String phone) {
        if (currentUser == null) {
            Log.e(TAG, "Cannot update data, currentUser is null");
            Toast.makeText(getContext(), "Error: Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> personal = new HashMap<>();
        // Only put non-null/non-default values if desired, or handle defaults appropriately
        personal.put("name", name); // Include name in the map to be saved
        personal.put("email", email);
        personal.put("phone", phone);

        // Use merge option to avoid overwriting other top-level fields if they exist
        Map<String, Object> updates = new HashMap<>();
        updates.put("Personal", personal);

        db.collection("Users")
                .document(currentUser.getUid())
                .set(updates, SetOptions.merge()) // Use set with merge
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Personal data updated successfully in Firestore");
                    // Update UI elements
                    tvUserName.setText(name); // Ensure name is updated if it was part of the change
                    tvUserEmail.setText(email);
                    tvUserPhone.setText(phone.isEmpty() ? getString(R.string.not_provided) : phone);

                    // Update the activity header as well
                    if (getActivity() instanceof ProfileActivity) {
                        ((ProfileActivity) getActivity()).updateHeaderName(name);
                    }

                    Toast.makeText(getContext(), "Contact information updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating personal data in Firestore", e);
                    Toast.makeText(getContext(), "Error updating details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Optional: Refresh data or sync header name if needed when fragment resumes
        // loadUserData(); // Uncomment if you want to reload data every time fragment resumes
        if (getActivity() instanceof ProfileActivity && tvUserName != null) {
            ProfileActivity activity = (ProfileActivity) getActivity();
            String headerName = activity.getHeaderName();
            // Sync TextView name with Activity header if they differ
            if (headerName != null && !headerName.isEmpty() &&
                    (tvUserName.getText() == null || !tvUserName.getText().toString().equals(headerName))) {
                tvUserName.setText(headerName);
            } else if (tvUserName.getText() != null && !tvUserName.getText().toString().isEmpty() &&
                       !tvUserName.getText().toString().equals(getString(R.string.not_available)) && // Don't sync "N/A" back
                       (headerName == null || !headerName.equals(tvUserName.getText().toString()))) {
                // Sync Activity header with TextView if header is outdated (and TextView isn't "N/A")
                activity.updateHeaderName(tvUserName.getText().toString());
            }
        }
        // Reload profile picture in case it was changed and fragment was paused/resumed
        loadProfilePic();
    }

    private void showImageSourceDialog() {
        Log.d(TAG, "showImageSourceDialog called");
        String[] options = { "Take Photo", "Choose from Gallery" };
        // Ensure context is not null
        if (getContext() == null) {
            Log.e(TAG, "Cannot show dialog, context is null");
            return;
        }
        new AlertDialog.Builder(requireContext()) // Use requireContext() for guaranteed non-null
                .setTitle("Change Profile Picture")
                .setItems(options, (dialog, which) -> {
                    Log.d(TAG, "Dialog option selected: " + which);
                    if (which == 0) {
                        pendingAction = 0; // Set action before checking permissions
                        checkAndRequestPermissions();
                    } else {
                        pendingAction = 1; // Set action before checking permissions
                        checkAndRequestPermissions();
                    }
                }).show();
    }

    private void checkAndRequestPermissions() {
        Log.d(TAG, "checkAndRequestPermissions called, pendingAction: " + pendingAction);
        if (getContext() == null || getActivity() == null) {
             Log.e(TAG, "Cannot check permissions, context or activity is null");
             pendingAction = -1;
             return;
        }

        String storagePermission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            storagePermission = Manifest.permission.READ_MEDIA_IMAGES;
            Log.d(TAG, "Requesting READ_MEDIA_IMAGES (Android 13+)");
        } else { // Below Android 13
            storagePermission = Manifest.permission.READ_EXTERNAL_STORAGE;
            Log.d(TAG, "Requesting READ_EXTERNAL_STORAGE (Below Android 13)");
        }

        // Always request Camera permission if needed for camera action
        // Only request storage permission if needed for gallery action
        String[] permissionsToRequest;
        if (pendingAction == 0) { // Camera
             permissionsToRequest = new String[]{ Manifest.permission.CAMERA };
             // Note: Camera might implicitly need storage on older devices, but FileProvider handles it.
             // If saving the camera image fails later, you might need storage permission too.
        } else if (pendingAction == 1) { // Gallery
             permissionsToRequest = new String[]{ storagePermission };
        } else {
             Log.w(TAG, "checkAndRequestPermissions called with invalid pendingAction: " + pendingAction);
             // Decide if you need to request both by default or handle error
             permissionsToRequest = new String[]{ Manifest.permission.CAMERA, storagePermission };
        }


        boolean allNeededPermissionsGranted = true;
        for (String perm : permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(requireContext(), perm) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission NOT granted: " + perm);
                allNeededPermissionsGranted = false;
                break;
            } else {
                 Log.d(TAG, "Permission already granted: " + perm);
            }
        }


        if (allNeededPermissionsGranted) {
            Log.d(TAG, "All necessary permissions already granted, proceeding.");
            if (pendingAction == 0) {
                openCamera();
            } else if (pendingAction == 1) {
                openGallery();
            }
            pendingAction = -1;
        } else {
            Log.d(TAG, "Not all necessary permissions granted, launching permission request...");
            if (requestPermissionLauncher != null) {
                // Request *only* the permissions needed for the current action
                requestPermissionLauncher.launch(permissionsToRequest);
            } else {
                Log.e(TAG, "requestPermissionLauncher is null, cannot request permissions.");
                Toast.makeText(getContext(), "Error requesting permissions.", Toast.LENGTH_SHORT).show();
                pendingAction = -1;
            }
        }
    }

    // REMOVED onRequestPermissionsResult - Handled by requestPermissionLauncher callback

    private void openCamera() {
        Log.d(TAG, "openCamera called");
        // Ensure context/activity is available
        if (getContext() == null || getActivity() == null) {
             Log.e(TAG, "Cannot open camera, context or activity is null");
             pendingAction = -1; // Reset action
             return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure there's an activity to handle the intent
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            Log.d(TAG, "Camera intent can be resolved.");
            File photoFile = null;
            try {
                photoFile = createImageFile();
                Log.d(TAG, "Image file created: " + photoFile.getAbsolutePath());
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
                Toast.makeText(getContext(), "Error creating file", Toast.LENGTH_SHORT).show();
                pendingAction = -1; // Reset action
                return; // Don't proceed if file creation failed
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                try {
                    // Get URI using FileProvider
                    cameraImageUri = FileProvider.getUriForFile(requireContext(),
                            requireContext().getPackageName() + ".provider", // Authority must match AndroidManifest.xml
                            photoFile);
                    Log.d(TAG, "FileProvider URI generated: " + cameraImageUri);

                    intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                    // Grant temporary read/write permissions to the receiving app (camera app)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    Log.d(TAG, "Launching camera intent...");
                    if (cameraLauncher != null) {
                        cameraLauncher.launch(intent); // Use the launcher
                    } else {
                         Log.e(TAG, "cameraLauncher is null, cannot launch camera.");
                         Toast.makeText(getContext(), "Error launching camera.", Toast.LENGTH_SHORT).show();
                         pendingAction = -1; // Reset action
                    }

                } catch (IllegalArgumentException e) {
                    // This usually means the FileProvider authority is incorrect or not declared
                    Log.e(TAG, "FileProvider error - check authorities in Manifest and file_paths.xml", e);
                    Toast.makeText(getContext(), "File Provider setup error", Toast.LENGTH_LONG).show();
                    pendingAction = -1; // Reset action
                } catch (Exception e) {
                    // Catch other potential exceptions during intent setup/launch
                    Log.e(TAG, "Error setting up or launching camera intent", e);
                    Toast.makeText(getContext(), "Could not launch camera", Toast.LENGTH_SHORT).show();
                    pendingAction = -1; // Reset action
                }
            } else {
                Log.e(TAG, "photoFile is null after trying to create it.");
                pendingAction = -1; // Reset action
            }
        } else {
            Log.e(TAG, "Cannot resolve camera intent. No camera app installed?");
            Toast.makeText(getContext(), "No camera app found", Toast.LENGTH_SHORT).show();
            pendingAction = -1; // Reset action
        }
    }

    private File createImageFile() throws IOException {
        // Ensure context is available
        if (getContext() == null) {
            throw new IOException("Context is null, cannot create image file");
        }
        // Create an image file name (can use timestamp for uniqueness if needed)
        String imageFileName = "profile_pic.jpg"; // Overwrites previous temp file
        File storageDir = requireContext().getFilesDir(); // Use app's internal files directory

        // Ensure the directory exists (though getFilesDir should already exist)
        if (!storageDir.exists() && !storageDir.mkdirs()) {
             Log.e(TAG, "Failed to create directory: " + storageDir.getAbsolutePath());
             throw new IOException("Failed to create directory for image");
        }

        File imageFile = new File(storageDir, imageFileName);
        Log.d(TAG, "Creating image file at: " + imageFile.getAbsolutePath());
        // Delete existing file to ensure camera overwrites it cleanly if needed
        if (imageFile.exists()) {
            if (!imageFile.delete()) {
                Log.w(TAG, "Could not delete existing temp image file.");
            }
        }
        return imageFile;
    }

    private void openGallery() {
        Log.d(TAG, "openGallery called");
        // Ensure context/activity is available
        if (getContext() == null || getActivity() == null) {
             Log.e(TAG, "Cannot open gallery, context or activity is null");
             pendingAction = -1; // Reset action
             return;
        }
        // Use ACTION_PICK for selecting existing content
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*"); // Specify image MIME type

        // Verify that there's an activity to handle the intent
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            Log.d(TAG, "Gallery intent can be resolved.");
            Log.d(TAG, "Launching gallery intent...");
            if (galleryLauncher != null) {
                galleryLauncher.launch(intent); // Use the launcher
            } else {
                 Log.e(TAG, "galleryLauncher is null, cannot launch gallery.");
                 Toast.makeText(getContext(), "Error launching gallery.", Toast.LENGTH_SHORT).show();
                 pendingAction = -1; // Reset action
            }
        } else {
            Log.e(TAG, "Cannot resolve gallery intent. No gallery app installed?");
            Toast.makeText(getContext(), "No gallery app found", Toast.LENGTH_SHORT).show();
            pendingAction = -1; // Reset action
        }
    }

    private void saveImageToLocal(Uri imageUri) {
        Log.d(TAG, "saveImageToLocal called with URI: " + imageUri);
        // Ensure context is available
        if (getContext() == null) {
            Log.e(TAG, "Cannot save image, context is null");
            return;
        }
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                 Log.e(TAG, "Failed to open input stream for URI: " + imageUri);
                 throw new IOException("Could not open input stream");
            }
            // Use the same filename as the camera to overwrite if necessary
            File file = new File(requireContext().getFilesDir(), "profile_pic.jpg");
            outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[4096]; // 4KB buffer
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            Log.d(TAG, "Image saved successfully to: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to save image to local storage", e);
            Toast.makeText(getContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
    }

    private void loadProfilePic() {
        Log.d(TAG, "loadProfilePic called");
        // Ensure context and ImageView are available
        if (getContext() == null || ivProfilePic == null) {
            Log.e(TAG, "Cannot load profile pic, context or ImageView is null");
            return;
        }
        File file = new File(requireContext().getFilesDir(), "profile_pic.jpg");
        if (file.exists() && file.length() > 0) { // Check if file exists and is not empty
            Log.d(TAG, "Loading profile picture from local file: " + file.getAbsolutePath());
            // Use a key that changes when the file changes to bust Glide's cache
            RequestOptions options = new RequestOptions() // <-- Corrected usage
                .signature(new ObjectKey(file.lastModified())) // Use the imported ObjectKey
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder); // Show placeholder on error too

            Glide.with(this) // Use 'this' (Fragment) for context
                 .load(Uri.fromFile(file))
                 .apply(options)
                 .into(ivProfilePic);

            // Also update activity header if needed
            if (getActivity() instanceof ProfileActivity) {
                ((ProfileActivity) getActivity()).updateProfilePictureFromLocal();
            }
        } else {
            if (file.exists()) {
                Log.w(TAG, "Local profile picture file exists but is empty: " + file.getAbsolutePath());
            } else {
                Log.d(TAG, "Local profile picture not found. Checking Google photo.");
            }
            // Load Google photo or placeholder
            String googlePhotoUrlString = null; // Rename to avoid confusion
            Uri googlePhotoUri = null; // Variable for the Uri
            if (currentUser != null && currentUser.getPhotoUrl() != null) {
                googlePhotoUri = currentUser.getPhotoUrl(); // Get the Uri directly
                googlePhotoUrlString = googlePhotoUri.toString(); // Keep the string for logging if needed
            }

            if (googlePhotoUri != null) { // Check the Uri, not the string
                Log.d(TAG, "Loading profile picture from Google URL: " + googlePhotoUrlString);
                RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder);

                Glide.with(this)
                        .load(googlePhotoUri) // Load using the Uri
                        .apply(options)
                        .into(ivProfilePic);
                if (getActivity() instanceof ProfileActivity) {
                    // Pass the Uri object to the activity method
                    ((ProfileActivity) getActivity()).updateProfilePictureFromGoogle(googlePhotoUri); // <-- Pass the Uri
                }
            } else {
                Log.d(TAG, "No Google photo found. Using placeholder.");
                 Glide.with(this)
                      .load(R.drawable.profile_placeholder)
                      .into(ivProfilePic);
                if (getActivity() instanceof ProfileActivity) {
                    ((ProfileActivity) getActivity()).updateProfilePictureToPlaceholder();
                }
            }
        }
    }
}