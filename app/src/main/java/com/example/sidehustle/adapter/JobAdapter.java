package com.example.sidehustle.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.sidehustle.JobDetailActivity;
import com.example.sidehustle.R;
import com.example.sidehustle.model.Job;

import java.util.List;

public class JobAdapter extends RecyclerView.Adapter<JobAdapter.JobViewHolder> {
    private Context context;
    private List<Job> jobs;

    public JobAdapter(Context context, List<Job> jobs) {
        this.context = context;
        this.jobs = jobs;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_featured, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job = jobs.get(position);
        holder.jobTitle.setText(job.getTitle());
        holder.companyName.setText(job.getCompany());
        holder.jobLocation.setText(job.getLocation());
        holder.jobSalary.setText(job.getSalary());

        String driveUrl = job.getImageUrl();
        if (driveUrl != null && !driveUrl.isEmpty()) {
            String directImageUrl = convertDriveUrlToDirectUrl(driveUrl);
            Log.d("ImageLoading", "Original URL: " + driveUrl);
            Log.d("ImageLoading", "Converted URL: " + directImageUrl);
            
            Glide.with(context)
                .load(directImageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                            Target<Drawable> target, boolean isFirstResource) {
                        Log.e("ImageLoading", "Failed to load image: " + model + 
                              "\nException: " + (e != null ? e.getMessage() : "null"), e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                            Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d("ImageLoading", "Image loaded successfully");
                        return false;
                    }
                })
                .timeout(60000) // Increase timeout to 60 seconds
                .centerCrop()
                .into(holder.companyLogo);
        } else {
            Log.w("ImageLoading", "No image URL provided");
            holder.companyLogo.setImageResource(R.drawable.placeholder_image);
        }
        
        // Set click listener to open job details
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, JobDetailActivity.class);
            
            // Pass job details to the detail activity
            intent.putExtra(JobDetailActivity.EXTRA_JOB_ID, job.getId());
            intent.putExtra(JobDetailActivity.EXTRA_JOB_TITLE, job.getTitle());
            intent.putExtra(JobDetailActivity.EXTRA_COMPANY_NAME, job.getCompany());
            intent.putExtra(JobDetailActivity.EXTRA_JOB_LOCATION, job.getLocation());
            intent.putExtra(JobDetailActivity.EXTRA_JOB_SALARY, job.getSalary());
            intent.putExtra(JobDetailActivity.EXTRA_JOB_IMAGE, job.getImageUrl());
            intent.putExtra(JobDetailActivity.EXTRA_JOB_DESCRIPTION, job.getDescription());
            intent.putExtra(JobDetailActivity.EXTRA_JOB_REQUIREMENTS, job.getRequirements());
            
            context.startActivity(intent);
        });

        // Add these lines for enhanced click feedback
        holder.itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Scale down slightly when pressed
                    v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Scale back to normal when released
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    
                    // For ACTION_UP (actual click), also navigate to details
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        Intent intent = new Intent(context, JobDetailActivity.class);
                        
                        // Pass job details to the detail activity
                        intent.putExtra(JobDetailActivity.EXTRA_JOB_ID, job.getId());
                        intent.putExtra(JobDetailActivity.EXTRA_JOB_TITLE, job.getTitle());
                        intent.putExtra(JobDetailActivity.EXTRA_COMPANY_NAME, job.getCompany());
                        intent.putExtra(JobDetailActivity.EXTRA_JOB_LOCATION, job.getLocation());
                        intent.putExtra(JobDetailActivity.EXTRA_JOB_SALARY, job.getSalary());
                        intent.putExtra(JobDetailActivity.EXTRA_JOB_IMAGE, job.getImageUrl());
                        intent.putExtra(JobDetailActivity.EXTRA_JOB_DESCRIPTION, job.getDescription());
                        intent.putExtra(JobDetailActivity.EXTRA_JOB_REQUIREMENTS, job.getRequirements());
                        
                        context.startActivity(intent);
                    }
                    break;
            }
            return true;
        });
    }

    private String convertDriveUrlToDirectUrl(String driveUrl) {
        String fileId = "";
        if (driveUrl.contains("drive.google.com/file/d/")) {
            fileId = driveUrl.split("/file/d/")[1].split("/")[0];
        } else if (driveUrl.contains("drive.google.com/open?id=")) {
            fileId = driveUrl.split("open\\?id=")[1];
        }
        
        // Using an alternative format that might work better on real devices
        return "https://lh3.googleusercontent.com/d/" + fileId;
    }

    @Override
    public int getItemCount() {
        return jobs.size();
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {
        ImageView companyLogo;
        TextView jobTitle, companyName, jobLocation, jobSalary;

        JobViewHolder(View itemView) {
            super(itemView);
            companyLogo = itemView.findViewById(R.id.companyLogo);
            jobTitle = itemView.findViewById(R.id.jobTitle);
            companyName = itemView.findViewById(R.id.companyName);
            jobLocation = itemView.findViewById(R.id.jobLocation);
            jobSalary = itemView.findViewById(R.id.jobSalary);
        }
    }
}
