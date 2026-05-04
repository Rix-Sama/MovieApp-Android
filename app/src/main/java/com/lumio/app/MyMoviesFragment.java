package com.lumio.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MyMoviesFragment extends Fragment {

    private RecyclerView recyclerView;
    private View emptyState;
    private TextView tvMovieCount;
    private LocalMovieAdapter adapter;
    private LocalMovieRepository repo;

    public static MyMoviesFragment newInstance() { return new MyMoviesFragment(); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_my_movies, container, false);

        recyclerView  = root.findViewById(R.id.recyclerViewLocal);
        emptyState    = root.findViewById(R.id.emptyState);
        tvMovieCount  = root.findViewById(R.id.tvMovieCount);

        repo = LocalMovieRepository.getInstance(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LocalMovieAdapter(repo.getMovies(), requireContext());
        adapter.setOnListChangedListener(this::updateState);
        recyclerView.setAdapter(adapter);

        updateState();
        return root;
    }

    @Override public void onResume() { super.onResume(); refreshList(); }

    public void refreshList() {
        if (adapter != null) { adapter.notifyDataSetChanged(); updateState(); }
    }

    private void updateState() {
        int count = repo.getMovies().size();
        if (tvMovieCount != null)
            tvMovieCount.setText(count + " film" + (count > 1 ? "s" : ""));
        if (count == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }
}
