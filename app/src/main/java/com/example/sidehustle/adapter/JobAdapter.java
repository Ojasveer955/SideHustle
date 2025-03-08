package com.example.sidehustle.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sidehustle.R;
import com.example.sidehustle.model.Job;

import java.util.List;

public class JobAdapter extends RecyclerView.Adapter<JobAdapter.JobViewHolder> {
    
    private List<Job> jobs;
    
    public JobAdapter(List<Job> jobs) {
        this.jobs = jobs;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_featured, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job = jobs.get(position);
        holder.jobTitle.setText(job.getTitle());
        holder.companyName.setText(job.getCompany());
        holder.jobLocation.setText(job.getLocation());
        holder.jobSalary.setText(job.getSalary());
        holder.companyLogo.setImageResource(job.getImageResourceId());
    }

    @Override
    public int getItemCount() {
        return jobs.size();
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {
        ImageView companyLogo;
        TextView jobTitle;
        TextView companyName;
        TextView jobLocation;
        TextView jobSalary;

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