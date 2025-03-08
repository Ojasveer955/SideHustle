package com.example.sidehustle.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

        // Load image from URL using Glide
        Glide.with(context)
                .load(job.getImageUrl())  // Load image from Firestore URL
                .placeholder(R.drawable.placeholder_image) // Add a placeholder image
                .error(R.drawable.error_image) // Add an error image if the URL is broken
                .into(holder.companyLogo);
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
