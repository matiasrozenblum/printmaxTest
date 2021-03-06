package com.example.matirozen.printmaxtest;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Adapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.matirozen.printmaxtest.Adapter.TypeAdapter;
import com.example.matirozen.printmaxtest.Database.Local.TagDAO;
import com.example.matirozen.printmaxtest.Database.ModelDB.Price;
import com.example.matirozen.printmaxtest.Database.ModelDB.TagDB;
import com.example.matirozen.printmaxtest.Model.Precio;
import com.example.matirozen.printmaxtest.Model.Tag;
import com.example.matirozen.printmaxtest.Retrofit.PrintmaxTestService;
import com.example.matirozen.printmaxtest.Utils.Listener;
import com.facebook.accountkit.AccountKit;
import com.nex3z.notificationbadge.NotificationBadge;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, Listener {

    TextView txtName, txtPhone;
    RecyclerView lst_tag;
    NotificationBadge badge;
    ImageView cartIcon;

    //RxJava
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        lst_tag = (RecyclerView)findViewById(R.id.recycler_tags);
        lst_tag.setLayoutManager(new GridLayoutManager(this, 2));
        lst_tag.setHasFixedSize(true);
        loadListTag("6");

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        txtName = (TextView)headerView.findViewById(R.id.txtName);
        txtPhone = (TextView)headerView.findViewById(R.id.txtPhone);

        //Set info
        txtName.setText(PrintmaxTestService.currentUser.getName());
        txtPhone.setText(PrintmaxTestService.currentUser.getPhone());
    }

    private void loadListTag(String menuId) {
        PrintmaxTestService.get().getAllPrices()
                .enqueue(new Callback<ArrayList<Precio>>() {
                    @Override
                    public void onResponse(Call<ArrayList<Precio>> call, Response<ArrayList<Precio>> response) {
                        PrintmaxTestService.priceRepository.nukeTable();
                        for(Precio precio : response.body()){
                            Price price = new Price();
                            price.codigo = precio.getCodigo();
                            price.precioa = Float.valueOf(precio.getprecioa());
                            price.preciob = Float.valueOf(precio.getpreciob());
                            price.precioc = Float.valueOf(precio.getprecioc());
                            price.preciod = Float.valueOf(precio.getpreciod());
                            price.precioe = Float.valueOf(precio.getprecioe());
                            PrintmaxTestService.priceRepository.insertIntoPrice(price);
                        }
                    }

                    @Override
                    public void onFailure(Call<ArrayList<Precio>> call, Throwable t) {
                        Log.d("hola", "hola");
                    }
                });
        if(PrintmaxTestService.tagRepository.getTags().size() > 0){
            List<Tag> tagList = new ArrayList<>();
            for(TagDB tagDB : PrintmaxTestService.tagRepository.getTags()){
                Tag tag = new Tag();
                tag.id = String.valueOf(tagDB.id);
                tag.name = tagDB.name;
                tag.link = tagDB.link;
                tagList.add(tag);
            }
            displayTagList(tagList);
        } else {
            PrintmaxTestService.get().getTag(menuId)
                    .enqueue(new Callback<List<Tag>>() {
                        @Override
                        public void onResponse(Call<List<Tag>> call, Response<List<Tag>> response) {
                            for(Tag tag : response.body()) {
                                TagDB tagDB = new TagDB();
                                tagDB.id = Integer.valueOf(tag.id);
                                tagDB.name = tag.name;
                                tagDB.link = tag.link;
                                PrintmaxTestService.tagRepository.insertIntoTagDB(tagDB);
                            }
                            displayTagList(response.body());
                        }

                        @Override
                        public void onFailure(Call<List<Tag>> call, Throwable t) {
                            Log.d("hola", "hola");
                        }
                    });
        }
    }

    private void displayTagList(List<Tag> tags) {
        TypeAdapter adapter = new TypeAdapter(this, tags);
        lst_tag.setAdapter(adapter);
        TypeAdapter typeAdapter = (TypeAdapter)lst_tag.getAdapter();
        typeAdapter.setListener(HomeActivity.this);
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_action_bar, menu);
        View view = menu.findItem(R.id.cartMenu).getActionView();
        badge = (NotificationBadge)view.findViewById(R.id.badge);
        cartIcon = (ImageView)view.findViewById(R.id.cartIcon);
        cartIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(HomeActivity.this, CartActivity.class));
            }
        });
        updateCartCount();
        return true;
    }

    private void updateCartCount() {
        if(badge == null) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(PrintmaxTestService.cartRepository.countCartItems() == 0){
                    badge.setVisibility(View.INVISIBLE);
                } else {
                    badge.setVisibility(View.VISIBLE);
                    badge.setText(String.valueOf(PrintmaxTestService.cartRepository.countCartItems()));
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.cartMenu) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if(id == R.id.sync){
            PrintmaxTestService.get().getTag("6").enqueue(new Callback<List<Tag>>() {
                @Override
                public void onResponse(Call<List<Tag>> call, Response<List<Tag>> response) {
                    PrintmaxTestService.tagRepository.nukeTable();
                    for(Tag tag : response.body()) {
                        TagDB tagDB = new TagDB();
                        tagDB.id = Integer.valueOf(tag.id);
                        tagDB.name = tag.name;
                        tagDB.link = tag.link;
                        PrintmaxTestService.tagRepository.insertIntoTagDB(tagDB);
                    }
                    displayTagList(response.body());
                }

                @Override
                public void onFailure(Call<List<Tag>> call, Throwable t) {
                    Log.d("hola", "hola");
                }
            });

            PrintmaxTestService.get().getAllPrices().enqueue(new Callback<ArrayList<Precio>>() {
                @Override
                public void onResponse(Call<ArrayList<Precio>> call, Response<ArrayList<Precio>> response) {
                    PrintmaxTestService.priceRepository.nukeTable();
                    for(Precio precio : response.body()){
                        Price price = new Price();
                        price.codigo = precio.getCodigo();
                        price.precioa = Float.valueOf(precio.getprecioa());
                        price.preciob = Float.valueOf(precio.getpreciob());
                        price.precioc = Float.valueOf(precio.getprecioc());
                        price.preciod = Float.valueOf(precio.getpreciod());
                        price.precioe = Float.valueOf(precio.getprecioe());
                        PrintmaxTestService.priceRepository.insertIntoPrice(price);
                    }
                }

                @Override
                public void onFailure(Call<ArrayList<Precio>> call, Throwable t) {
                    Log.d("hola", "hola");
                }
            });

            Toast.makeText(HomeActivity.this, "Datos sincronizados", Toast.LENGTH_SHORT).show();
        }

        if (id == R.id.nav_sign_out) {
            // Create confirm dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Exit Application");
            builder.setMessage("¿Esta seguro que quiere salir?");

            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AccountKit.logOut();

                    //Clear all activity
                    Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            builder.show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartCount();
    }

    public void update() {
        updateCartCount();
    }
}
