package com.example.alec.positive_eating;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import shaneconnect.ConcreteCommand;
import shaneconnect.DeleteOrders;
import shaneconnect.ShaneConnect;

import static android.view.Gravity.CENTER;
import static com.example.alec.positive_eating.Singleton_ShaneConnect_Factory.getShaneConnect;

public class CookOrderList extends AppCompatActivity {
    private List<String> listDataHeader;
    private HashMap<String, List<String>> listDataChild;
    private ArrayList<JSONObject> orders, foodList;
    private ShaneConnect ModelM;
    private int recursiveInc;
    private int bufferOrderToDelete;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cook_order_list);
        View someView = findViewById(R.id.activity_cook_order_list);
        someView.setBackgroundColor(Color.BLUE);
        context = getApplicationContext();
        bufferOrderToDelete = -1;
        prepareListData();
    }

    private void prepareListData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();
        Toast.makeText(getApplicationContext(), "Loading. This may take several seconds.",
                Toast.LENGTH_LONG).show();
        createFoodList();
    }

    private void createFoodList() {
        ModelM = getShaneConnect();
        foodList = new ArrayList<JSONObject>();

            foodList.add(null);


        createFoodList(0);
    }

    private void createFoodList(final int i) {
        ModelM.getFoodByIndex(i,new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject response){
                if(!response.has("none")) {
                    foodList.add(response);
                    createFoodList(i+1);
                } else {
                    getAllOrders();
                }
            }
        });
    }

    private void setup() {
        ExpandableListView expListView = (ExpandableListView) findViewById(R.id.lvExp);
        CookListAdapter listAdapter = new CookListAdapter(context, listDataHeader, listDataChild);
        expListView.setAdapter(listAdapter);

        TextView text = new TextView(this);
        text.setText(String.valueOf("Are you sure this order is complete?"));
        text.setGravity(CENTER);

        final AlertDialog.Builder builder= new AlertDialog.Builder(this);
        builder.setTitle("Finished With Order?");
        builder.setView(text);

        builder.setPositiveButton("Yes (Delete order)", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            beginRemoveOrder();
            }
        });
        builder.setNegativeButton("No (Go back)", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
            }
        });
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView listview, View view, int groupPos,
                                        int childPos, long id) {
                bufferOrderToDelete = groupPos;
                builder.show();
                return false;
            }
        });
    }

    private void beginRemoveOrder() {
        String descriptions = null;
        try {
            descriptions = orders.get(bufferOrderToDelete).getString("desc");
        } catch (Exception e){
            Toast.makeText(context, "A very weird error occurred in beginRemoveOrder...",
                    Toast.LENGTH_LONG).show();
        }
        if(bufferOrderToDelete<0 || descriptions==null)
            return;
        ConcreteCommand c = new ConcreteCommand();
        final String desc = descriptions;
        new DeleteOrders(ModelM, desc, c).exectute(new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Toast.makeText(context, "Successfully deleted "+ desc, Toast.LENGTH_LONG).show();
                finishActivity(0);
                startActivity(new Intent(context, CookOrderList.class));
            }
        });
    }

    private void processOrders(final int i) {
        try {
            String compString = orders.get(i).getString("componentString");
            final String orderNum = "Order #" + orders.get(i).getInt("order_id");
            ModelM.getFoodByID(compString, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        ArrayList<String> orderInfo = parseResponse(response);
                        listDataHeader.add(orderNum);
                        listDataChild.put(orderNum, orderInfo);
                        if (i == orders.size() - 1) {
                            setup();
                        } else {
                            processOrders(i + 1);
                        }
                    } catch (Exception e) {
                        Toast.makeText(context, "An error occurred in getFoodByID" +
                                        "Please press the back button and try again.",
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(context, "An error occurred in getOrders. " +
                            "Please press the back button and try again.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void getAllOrders() {
        ModelM = getShaneConnect();
        recursiveInc = 0;
        orders = new ArrayList<JSONObject>();
        ModelM.getOrders(recursiveInc, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse (JSONObject response) {
                if (response.has("none")) {
                    processOrders();
                } else {
                    orders.add(response);
                    ModelM.getOrders(++recursiveInc, this);
                }
            }
        });
    }

    private void processOrders() {
        for(int i=0;i<orders.size();i++) {
            try {
                ArrayList<String> orderInfo = new ArrayList<String>();
                JSONObject json = orders.get(i);
                final String orderNum = "Order #" + orders.get(i).getInt("order_id");
                String comp = json.getString("componentString");
                Scanner scan = new Scanner(comp);
                scan.useDelimiter("-");
                while(scan.hasNext()) {
                    String token = scan.next();
                    int openPar = token.indexOf("(");
                    int closePar = token.lastIndexOf(")");
                    int foodIndex = Integer.parseInt(token.substring(0,openPar));
                    String foodName = foodList.get(foodIndex).getString("desc")+"\n" +"Options:\n";
                    String options = token.substring(openPar+1, closePar);
                    foodName+=options;
                    orderInfo.add(foodName);
                }
                listDataHeader.add(orderNum);
                listDataChild.put(orderNum, orderInfo);
            } catch (JSONException e) {
                Toast.makeText(getApplicationContext(),e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        setup();
    }

    private ArrayList<String> parseResponse(JSONObject res) throws JSONException {
        ArrayList<String> order = new ArrayList<String>();
        for(int i =0;res.has("NAME"+i);i++) {
            try {
                String name = "";
                name+=res.getString("DESCR" + i)+"\n";
                name+="Quantity:" + res.getInt("QUANTITY" + i)+"\n";
                if(res.has("OPTIONS"+i)) {
                    JSONObject jsonOptions = res.getJSONObject("OPTIONS" + i);
                    name += "Options:";
                    for (int j = 0; jsonOptions.has("OPTION" + j); j++) {
                        name += "\n\t"+ jsonOptions.getString("OPTION" + j);
                    }
                }
                order.add(name);
            } catch (Exception e) {
                throw e;
            }
        }
        return order;
    }
}