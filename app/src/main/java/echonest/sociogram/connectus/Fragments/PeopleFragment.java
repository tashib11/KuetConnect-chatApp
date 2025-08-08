package echonest.sociogram.connectus.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import echonest.sociogram.connectus.Adapters.AdapterUsers;
import echonest.sociogram.connectus.Models.ModelUser;
import com.example.connectus.R;
import echonest.sociogram.connectus.SignInActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PeopleFragment extends Fragment {

    FirebaseAuth firebaseAuth;
    AdapterUsers adapterUsers;
    RecyclerView recyclerView;
    List<ModelUser> userList;

    public PeopleFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        firebaseAuth = FirebaseAuth.getInstance();

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_people, container, false);

        recyclerView = view.findViewById(R.id.usersRecyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // init user list and adapter ONCE
        userList = new ArrayList<>();
        adapterUsers = new AdapterUsers(getActivity(), userList);
        recyclerView.setAdapter(adapterUsers);

        // load users
        getAllUsers();

        return view;
    }

    private void getAllUsers() {
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) {
            // Not signed in — redirect to sign in safely if fragment attached
            if (isAdded() && getActivity() != null) {
                startActivity(new Intent(getActivity(), SignInActivity.class));
                getActivity().finish();
            }
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return; // fragment not attached

                userList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelUser modelUser = ds.getValue(ModelUser.class);
                    if (modelUser == null) continue; // skip nulls

                    String userId = modelUser.getUserId();
                    // ensure userId and fUser are non-null before equals
                    if (userId != null && fUser.getUid() != null && !userId.equals(fUser.getUid())) {
                        userList.add(modelUser);
                    }
                }

                // notify adapter once after loop
                adapterUsers.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // optional: log or show message
            }
        });
    }

    private void searchUsers(String query) {
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                userList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelUser modelUser = ds.getValue(ModelUser.class);
                    if (modelUser == null) continue;

                    String uid = modelUser.getUserId();
                    String name = modelUser.getName();
                    String email = modelUser.getEmail();

                    // Skip if uid null or is current user
                    if (uid == null || fUser.getUid() == null || uid.equals(fUser.getUid())) continue;

                    // Null-safe checks for name/email matching
                    boolean matches = false;
                    if (name != null && name.toLowerCase().contains(query.toLowerCase())) matches = true;
                    if (!matches && email != null && email.toLowerCase().contains(query.toLowerCase())) matches = true;

                    if (matches) {
                        userList.add(modelUser);
                    }
                }
                // update adapter once
                adapterUsers.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true); // to show menu in fragment
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);

        MenuItem menuItem = menu.findItem(R.id.action_search);
        // use AppCompat SearchView
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) menuItem.getActionView();
        searchView.setQueryHint("Search by name");

        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (!TextUtils.isEmpty(s.trim())) {
                    searchUsers(s);
                } else {
                    getAllUsers();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (!TextUtils.isEmpty(s.trim())) {
                    searchUsers(s);
                } else {
                    getAllUsers();
                }
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.logout) {
            firebaseAuth.signOut();
            checkUserStatus();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            if (isAdded() && getActivity() != null) {
                startActivity(new Intent(getActivity(), SignInActivity.class));
                getActivity().finish();
            }
        }
    }
}
