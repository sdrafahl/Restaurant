/**
 * @author Alec
 */
package com.example.alec.positive_eating;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;

/**
 * This class will be used to manage and get current workers.
 */
public class EditStaff extends ActionBarActivity {
    private static ExpandableListView expandableListView;
    private static ExpandableListAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_staff);
        Button add = (Button) findViewById(R.id.Add_Employee);
        //Initilizes the buttons.

        add.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view){
                        Intent myIntent = new Intent(EditStaff.this, add_employee.class); /** Class name here */
                        EditStaff.this.startActivity(myIntent);
                    }
                }
        );
        


    }



}
