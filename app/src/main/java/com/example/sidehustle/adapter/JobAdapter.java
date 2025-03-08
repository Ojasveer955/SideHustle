package com.example.sidehustle.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
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
