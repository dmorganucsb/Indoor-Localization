package ece596.ucsb.hfhmap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;


public class AngleWRTNDialog extends DialogFragment {
	
	public static double angle;
	
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View TrainingView = inflater.inflate(R.layout.fragment_enter_angle, null);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final EditText angle_text = (EditText)TrainingView.findViewById(R.id.angle_WRTN);
        builder.setView(TrainingView)
               .setPositiveButton(R.string.done_str, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   angle = Double.parseDouble(angle_text.getText().toString());
                	   // start 10 step training sequence
                       ((MainActivity)getActivity()).inputAngleWRTN(angle);
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }
    
    public double convertToMetric(double feet, double inches){
    	return ((feet*12 + inches)/39.370);
    }
}