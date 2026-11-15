package com.example.sharecare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sharecare.R;
import com.example.sharecare.models.Notification;
import com.example.sharecare.utils.NotificationUi;
import com.example.sharecare.utils.TimeUi;

import java.util.List;

public class NotificationAdapter
        extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final List<Notification> notifications;

    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onClick(Notification notification);
    }

    public NotificationAdapter(
            List<Notification> notifications,
            OnNotificationClickListener listener
    ) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        Notification notification = notifications.get(position);

        String type = notification.getType();

        holder.layoutIcon.setBackgroundResource(
                NotificationUi.circleRes(type)
        );

        holder.ivIcon.setImageResource(
                NotificationUi.iconRes(type)
        );

        holder.ivIcon.setColorFilter(
                ContextCompat.getColor(
                        holder.itemView.getContext(),
                        NotificationUi.tintRes(type)
                )
        );

        holder.tvTitle.setText(notification.getTitle());

        holder.tvContent.setText(notification.getContent());

        holder.viewUnread.setVisibility(
                notification.isRead() ? View.INVISIBLE : View.VISIBLE
        );

        holder.tvTime.setText(
                TimeUi.relative(notification.getCreatedAt())
        );

        holder.itemView.setOnClickListener(v ->
                listener.onClick(notification)
        );
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvContent, tvTime;
        View viewUnread;
        FrameLayout layoutIcon;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(
                    R.id.tvNotificationTitle
            );

            tvContent = itemView.findViewById(
                    R.id.tvNotificationContent
            );

            tvTime = itemView.findViewById(
                    R.id.tvNotificationTime
            );

            viewUnread = itemView.findViewById(
                    R.id.viewUnread
            );

            layoutIcon = itemView.findViewById(
                    R.id.layoutNotificationIcon
            );

            ivIcon = itemView.findViewById(
                    R.id.ivNotificationIcon
            );
        }
    }
}
