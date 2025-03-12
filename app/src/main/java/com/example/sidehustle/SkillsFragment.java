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

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillsFragment extends Fragment {

    private static final String TAG = "SkillsFragment";
    private ChipGroup skillsChipGroup;
    private TextView tvNoSkills;
    private Button btnAddSkill;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private List<String> userSkills = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_skills, container, false);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        // Initialize views
        skillsChipGroup = view.findViewById(R.id.skills_chip_group);
        tvNoSkills = view.findViewById(R.id.tv_no_skills);
        btnAddSkill = view.findViewById(R.id.btn_add_skill);
        
        // Setup button listener
        btnAddSkill.setOnClickListener(v -> showAddSkillDialog());
        
        // Load skills
        loadSkills();
        
        return view;
    }
    
    private void loadSkills() {
        if (currentUser == null) return;
        
        db.collection("Users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> skills = (List<String>) documentSnapshot.get("Skills");
                        
                        if (skills != null && !skills.isEmpty()) {
                            userSkills = skills;
                            displaySkills();
                        } else {
                            showNoSkillsMessage();
                        }
                    } else {
                        showNoSkillsMessage();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading skills", e);
                    Toast.makeText(getContext(), "Error loading skills", Toast.LENGTH_SHORT).show();
                    showNoSkillsMessage();
                });
    }
    
    private void displaySkills() {
        skillsChipGroup.removeAllViews();
        
        if (userSkills.isEmpty()) {
            showNoSkillsMessage();
            return;
        }
        
        tvNoSkills.setVisibility(View.GONE);
        
        for (String skill : userSkills) {
            Chip chip = new Chip(requireContext());
            chip.setText(skill);
            chip.setCloseIconVisible(true);
            chip.setCheckable(false);
            
            chip.setOnCloseIconClickListener(v -> {
                // Remove skill
                removeSkill(skill);
            });
            
            skillsChipGroup.addView(chip);
        }
    }
    
    private void showNoSkillsMessage() {
        skillsChipGroup.removeAllViews();
        tvNoSkills.setVisibility(View.VISIBLE);
    }
    
    private void showAddSkillDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_skills, null);
        
        TextInputEditText skillInput = dialogView.findViewById(R.id.et_skill);
        
        builder.setTitle("Add Skills")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String skillsText = skillInput.getText().toString().trim();
                    
                    if (skillsText.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter at least one skill", 
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Split by comma to allow multiple skills
                    String[] skillsArray = skillsText.split(",");
                    List<String> newSkills = new ArrayList<>();
                    
                    for (String skill : skillsArray) {
                        String trimmedSkill = skill.trim();
                        if (!trimmedSkill.isEmpty() && !userSkills.contains(trimmedSkill)) {
                            newSkills.add(trimmedSkill);
                        }
                    }
                    
                    if (newSkills.isEmpty()) {
                        Toast.makeText(getContext(), "No new skills to add", 
                                Toast.LENGTH_SHORT).show();
                    } else {
                        addSkills(newSkills);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
    
    private void addSkills(List<String> newSkills) {
        if (currentUser == null) return;
        
        // Add new skills to existing ones
        userSkills.addAll(newSkills);
        
        // Update Firestore
        db.collection("Users")
                .document(currentUser.getUid())
                .update("Skills", userSkills)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Skills added successfully", 
                            Toast.LENGTH_SHORT).show();
                    displaySkills();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding skills", e);
                    Toast.makeText(getContext(), "Error adding skills: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    // Remove the skills we tried to add
                    userSkills.removeAll(newSkills);
                });
    }
    
    private void removeSkill(String skillToRemove) {
        if (currentUser == null) return;
        
        // Remove from local list
        userSkills.remove(skillToRemove);
        
        // Update Firestore
        db.collection("Users")
                .document(currentUser.getUid())
                .update("Skills", userSkills)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Skill removed", Toast.LENGTH_SHORT).show();
                    displaySkills();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing skill", e);
                    Toast.makeText(getContext(), "Error removing skill: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    // Add the skill back to our local list since the operation failed
                    userSkills.add(skillToRemove);
                });
    }
}