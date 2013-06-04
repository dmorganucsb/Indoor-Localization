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


public class StepLenDialog extends DialogFragment {
	
	public static double step_len;
	
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View TrainingView = inflater.inflate(R.layout.step_len, null);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final EditText step_len_text = (EditText)TrainingView.findViewById(R.id.enter_step_len);
        builder.setView(TrainingView)
               .setPositiveButton(R.string.done_str, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   step_len = Double.parseDouble(step_len_text.getText().toString());
                       ((MainActivity)getActivity()).inputStepLength(step_len);
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }
    
    public double convertToMetric(double feet, double inches){
    	return ((feet*12 + inches)/39.370);
    }
}