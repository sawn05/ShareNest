package com.example.sharecare.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sharecare.R;
import com.example.sharecare.models.Request;
import com.example.sharecare.utils.ProductImageUi;
import com.example.sharecare.utils.RequestStatusUi;
import com.example.sharecare.utils.TimeUi;

import java.util.List;

public class RequestAdapter
        extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {

    public interface RequestListener {
        void onAccept(Request request);
        void onReject(Request request);
        void onDetail(Request request);
    }

    private final List<Request> requests;
    private final RequestListener listener;
    private final boolean isOwner;

    public RequestAdapter(
            List<Request> requests,
            boolean isOwner,
            RequestListener listener
    ) {
        this.requests = requests;
        this.isOwner = isOwner;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        Request request = requests.get(position);

        if (request.getProductName() != null
                && !request.getProductName().isEmpty()) {
            holder.tvProductName.setText(request.getProductName());
        } else if (request.getProductId() == null
                || request.getProductId().isEmpty()) {
            holder.tvProductName.setText("Nhu yếu phẩm");
        } else {
            holder.tvProductName.setText(
                    "Nhu yếu phẩm #" + request.getProductId()
            );
        }

        ProductImageUi.load(
                holder.ivProduct,
                request.getProductImageUrl(),
                request.getProductImageRes()
        );

        String person = isOwner
                ? request.getRequesterName()
                : request.getOwnerName();

        if (isOwner && (person == null || person.isEmpty())) {
            person = request.getRequesterId();
        }

        if (!isOwner && (person == null || person.isEmpty())) {
            person = request.getOwnerId();
        }

        if (person == null || person.isEmpty()) {
            person = "Không xác định";
        }

        holder.tvRequester.setText(
                holder.itemView.getContext().getString(
                        isOwner
                                ? R.string.requester_prefix
                                : R.string.owner_prefix,
                        person
                )
        );

        holder.tvMessage.setText(
                request.getMessage() == null
                        || request.getMessage().trim().isEmpty()
                        ? holder.itemView.getContext().getString(
                        R.string.no_message
                )
                        : request.getMessage()
        );

        holder.tvTime.setText(
                TimeUi.relative(request.getCreatedAt())
        );

        String status = request.getStatus();

        holder.tvStatus.setText(RequestStatusUi.label(status));
        holder.tvStatus.setBackgroundResource(
                RequestStatusUi.backgroundRes(status)
        );
        holder.tvStatus.setTextColor(
                ContextCompat.getColor(
                        holder.itemView.getContext(),
                        RequestStatusUi.textColorRes(status)
                )
        );

        if (isOwner && "PENDING".equals(status)) {

            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);

        } else {

            holder.btnAccept.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
        }

        holder.btnAccept.setOnClickListener(v ->
                listener.onAccept(request)
        );

        holder.btnReject.setOnClickListener(v ->
                listener.onReject(request)
        );

        holder.itemView.setOnClickListener(v ->
                listener.onDetail(request)
        );
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivProduct;
        TextView tvProductName, tvRequester, tvMessage, tvTime, tvStatus;
        Button btnAccept, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivProduct = itemView.findViewById(
                    R.id.imgRequestProduct
            );

            tvProductName = itemView.findViewById(
                    R.id.tvRequestProductName
            );
            tvRequester = itemView.findViewById(
                    R.id.tvRequestRequester
            );
            tvMessage = itemView.findViewById(R.id.tvRequestMessage);
            tvTime = itemView.findViewById(R.id.tvRequestTime);
            tvStatus = itemView.findViewById(R.id.tvRequestStatus);

            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
