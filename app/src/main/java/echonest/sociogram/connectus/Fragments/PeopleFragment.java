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
        firebaseAuth= FirebaseAuth.getInstance();
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_people, container, false);
        recyclerView= view.findViewById(R.id.usersRecyclerView);
//        recyclerView.setHasFixedSize(true);
//        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        //init user list
        userList = new ArrayList<>();
        getAllUsers();
        return  view;
    }

    private void getAllUsers() {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    // Fetch user data
                    String userId = ds.child("userId").getValue(String.class);
                    String name = ds.child("name").getValue(String.class);
                    String email = ds.child("email").getValue(String.class);
                    String profilePhoto = ds.child("profilePhoto").getValue(String.class);
                    String coverPhoto = ds.child("coverPhoto").getValue(String.class);
                    String onlineStatus = ds.child("onlineStatus").getValue(String.class);
                    String profession = ds.child("profession").getValue(String.class);
//                    int followerCount = ds.child("followerCount").getValue(Integer.class);
//                    if (followerCount == null) {
//                        followerCount = 0; // Default to 0 if it's null
//                    }

                    // Exclude current user
                    if (userId != null && !userId.equals(fUser.getUid())) {
                        ModelUser modelUser = new ModelUser(
                                coverPhoto, profilePhoto, email, null, null,
                                userId, name, onlineStatus, profession, 0,null
                        );
                        userList.add(modelUser);
                    }
                }
                // Initialize adapter and set to RecyclerView
                adapterUsers = new AdapterUsers(getActivity(), userList);
                recyclerView.setAdapter(adapterUsers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void searchUsers(String query) {
        //get current user
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        //get path of datbase named "Users" containing users info
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        // get all data from path
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelUser modelUser = ds.getValue(ModelUser.class);
                    //get all serached users except currently signed in user
                    if (!modelUser.getUserId().equals(fUser.getUid())) {
                        if(modelUser.getName().toLowerCase().contains(query.toLowerCase()) ||
                        modelUser.getEmail().toLowerCase().contains(query.toLowerCase())){
                            userList.add(modelUser);
                        }
                    }
                    //adapter
                    adapterUsers = new AdapterUsers(getActivity(), userList);
                    //refresh adawpter
                    adapterUsers.notifyDataSetChanged();
                    //set adapter to recycler view
                    recyclerView.setAdapter((adapterUsers));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private  void checkUserStatus(){
        FirebaseUser user= firebaseAuth.getCurrentUser();
    if(user!=null){

    }else{
        startActivity(new Intent(getActivity(), SignInActivity.class));
        getActivity().finish();
    }
    }

    public void onCreate(Bundle savedInstanceState){
        setHasOptionsMenu(true); //to show menu option in fragment
        super.onCreate(savedInstanceState);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);

        // Get the SearchView from the menu item
        MenuItem menuItem=menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView=(androidx.appcompat.widget.SearchView) menuItem.getActionView();
        searchView.setQueryHint("Search by name");


        // Set up the query text listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //called when user press search buttpm from  keyboard
                //if search query os npt empty then search
                if(!TextUtils.isEmpty(s.trim())){
                    searchUsers(s);
                }else{
                    //search text empty  ,get all users
                    getAllUsers();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {

                //if search query os npt empty then search
                if(!TextUtils.isEmpty(s.trim())){
                    searchUsers(s);
                }else{
                    //search text empty  ,get all users
                    getAllUsers();
                }
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    public boolean onOptionItemSelected(MenuItem item){
        int id =item.getItemId();
        if(id==R.id.logout) {
            firebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}