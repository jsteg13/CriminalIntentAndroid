package com.bignerdranch.android.criminalintent;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


import com.bignerdranch.android.criminalintent.database.TinyDB;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;

public class CrimeFragment extends Fragment {

    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_PHOTO = 2;


    private Context context;


    private Crime mCrime;
    private File mPhotoFile;
    private EditText mTitleField;
    private Button mDateButton;
    private CheckBox mSolvedCheckbox;

    private CheckBox mFaceDetection;
    private  TextView faceDetectView;

    private Button mReportButton;
    private Button mSuspectButton;
    private ImageButton mPhotoButton;
    private ImageView mPhotoView;

    private ImageView mPhotoView2;
    private ImageView mPhotoView3;
    private ImageView mPhotoView4;
    private CheckBox faceCheckBox;
    private TextView faceText;

    private ArrayList<String> filePaths;
    private ArrayList<Uri> uriList;
    private Integer count;

    SharedPreferences shared;





    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime);

    }

    @Override
    public void onPause() {
        super.onPause();

        CrimeLab.get(getActivity())
                .updateCrime(mCrime);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_crime, container, false);

        mTitleField = (EditText) v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());

        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mDateButton = (Button) v.findViewById(R.id.crime_date);
        updateDate();
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                DatePickerFragment dialog = DatePickerFragment
                        .newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                dialog.show(manager, DIALOG_DATE);
            }
        });

        mSolvedCheckbox = (CheckBox) v.findViewById(R.id.crime_solved);
        mSolvedCheckbox.setChecked(mCrime.isSolved());
        mSolvedCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
            }
        });

        faceDetectView = (TextView) v.findViewById(R.id.faceDetectTextView);
        faceDetectView.setText("");
        mFaceDetection = (CheckBox) v.findViewById(R.id.faceDetectCheckBox);
        mFaceDetection.setChecked(true);
        mFaceDetection.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked==true){
                    //Do Face Detection
                    faceDetectView.setText("Doing Face Detection");
                }
                else{
                    //Remove Textview fro face detection
                    faceDetectView.setText("");
                }
            }
        });

        mReportButton = (Button) v.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
                i.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.crime_report_subject));
                i = Intent.createChooser(i, getString(R.string.send_report));

                startActivity(i);
            }
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton = (Button) v.findViewById(R.id.crime_suspect);
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });

        if (mCrime.getSuspect() != null) {
            mSuspectButton.setText(mCrime.getSuspect());
        }

        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.resolveActivity(pickContact,
                PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        }

        mPhotoButton = (ImageButton) v.findViewById(R.id.crime_camera);
        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        boolean canTakePhoto = mPhotoFile != null &&
                captureImage.resolveActivity(packageManager) != null;
        mPhotoButton.setEnabled(canTakePhoto);

        if (canTakePhoto) {
            Uri uri = Uri.fromFile(mPhotoFile);
            captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }

        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(captureImage, REQUEST_PHOTO);
            }
        });

        filePaths = new ArrayList<>(8);
        count = 0;
        uriList = new ArrayList<>(8);
        shared = getActivity().getSharedPreferences("App_settings", MODE_PRIVATE);


        // Load the paths
        saveArrayList(filePaths,mCrime + "File-Paths");
        filePaths = getArrayList(mCrime + "File-Paths");

        uriList = getUrisFromFilePaths(filePaths);


        mPhotoView = (ImageView) v.findViewById(R.id.crime_photo);
        mPhotoView2 = (ImageView) v.findViewById(R.id.crime_photo2);
        mPhotoView3 = (ImageView) v.findViewById(R.id.crime_photo3);
        mPhotoView4 = (ImageView) v.findViewById(R.id.crime_photo4);

        try {
            updatePhotoView();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Save the paths
        filePaths = getPathsFromUri(uriList);
        saveArrayList(filePaths,mCrime + "File-Paths");

        if (count == 4){
            count = 0;
        }


        return v;

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_DATE) {
            Date date = (Date) data
                    .getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            updateDate();
        } else if (requestCode == REQUEST_CONTACT && data != null) {
            Uri contactUri = data.getData();
            // Specify which fields you want your query to return
            // values for.
            String[] queryFields = new String[]{
                    ContactsContract.Contacts.DISPLAY_NAME,
            };
            // Perform your query - the contactUri is like a "where"
            // clause here
            ContentResolver resolver = getActivity().getContentResolver();
            Cursor c = resolver
                    .query(contactUri, queryFields, null, null, null);

            try {
                // Double-check that you actually got results
                if (c.getCount() == 0) {
                    return;
                }

                // Pull out the first column of the first row of data -
                // that is your suspect's name.
                c.moveToFirst();

                String suspect = c.getString(0);
                mCrime.setSuspect(suspect);
                mSuspectButton.setText(suspect);
            } finally {
                c.close();
            }
        } else if (requestCode == REQUEST_PHOTO) {
            try {
                updatePhotoView();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateDate() {
        mDateButton.setText(mCrime.getDate().toString());
    }

    private String getCrimeReport() {
        String solvedString = null;
        if (mCrime.isSolved()) {
            solvedString = getString(R.string.crime_report_solved);
        } else {
            solvedString = getString(R.string.crime_report_unsolved);
        }
        String dateFormat = "EEE, MMM dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();
        String suspect = mCrime.getSuspect();
        if (suspect == null) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }
        String report = getString(R.string.crime_report,
                mCrime.getTitle(), dateString, solvedString, suspect);
        return report;
    }

    private void updatePhotoView() throws IOException {

        if (mPhotoFile == null || !mPhotoFile.exists()) {
            mPhotoView.setImageDrawable(null);
            mPhotoView2.setImageDrawable(null);
            mPhotoView3.setImageDrawable(null);
            mPhotoView4.setImageDrawable(null);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    mPhotoFile.getPath(), getActivity());
            Log.d("myTag", mPhotoFile.getPath());


            saveBitmap(bitmap);
            mPhotoView.setImageBitmap(bitmap);
            updateImageViews();
            Log.d("uris", String.valueOf(uriList.size()));

        }
    }

    public File saveBitmap(Bitmap bmp) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, bytes);

        File f = new File(Environment.getExternalStorageDirectory()
                + File.separator  + count + mCrime.getId()  + "crimeimage.jpg" );
        f.createNewFile();
        Uri uri = Uri.parse(f.getPath());
        Log.d("Uri", uri.getPath());
        uriList.add(uri);
        count = count + 1;
        FileOutputStream fo = new FileOutputStream(f);
        fo.write(bytes.toByteArray());
        fo.close();
        return f;
    }

    public ArrayList<String> getPathsFromUri (ArrayList<Uri> uriArrayList){
        ArrayList<String> pathsFromUri = new ArrayList<>();

        for (int i =0; i< uriArrayList.size();i++){
            Uri uri = uriArrayList.get(i);
            File myFile = new File(uri.getPath());
            pathsFromUri.add(myFile.getPath());
        }
        return pathsFromUri;
    }
    public ArrayList<Uri> getUrisFromFilePaths(ArrayList<String> filePaths){
        List<Uri> uris = new ArrayList<>();

        for (int i = 0; i<filePaths.size(); i++){
            Uri uri = Uri.parse(filePaths.get(i));
            uris.add(uri);
        }
        return (ArrayList<Uri>) uris;
    }
    private void updateImageViews (){

        Log.d("myTag", String.valueOf(uriList.size()));

        ArrayList<Bitmap> bitmaps = new ArrayList<>(8);

        for (int i =0; i< uriList.size();i++){
            Uri uri = uriList.get(i);
            File myFile = new File(uri.getPath());
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    myFile.getPath(), getActivity());
            bitmaps.add(bitmap);

        }
        if (bitmaps.size() == 5 ) {
            if (bitmaps.get(4) != null) {
                bitmaps.set(0, bitmaps.get(4));
                uriList.set(0, uriList.get(4));
            }
        }
        if (bitmaps.size() == 6 ) {
            if (bitmaps.get(5) != null) {
                bitmaps.set(1, bitmaps.get(5));
                uriList.set(1, uriList.get(5));
            }
        }
        if (bitmaps.size() == 7 ) {
            if (bitmaps.get(6) != null) {
                bitmaps.set(2, bitmaps.get(6));
                uriList.set(2, uriList.get(6));
            }
        }
        if (bitmaps.size() == 8 ) {
            if (bitmaps.get(7) != null) {
                bitmaps.set(3, bitmaps.get(7));
                uriList.set(3, uriList.get(7));
                bitmaps.remove(4);
                bitmaps.remove(5);
                bitmaps.remove(6);
                bitmaps.remove(7);
                uriList.remove(4);
                uriList.remove(5);
                uriList.remove(6);
                uriList.remove(7);
            }
        }

        if (bitmaps.get(0) == null){
            mPhotoView.setImageDrawable(null);
        }else {
            mPhotoView.setImageBitmap(bitmaps.get(0));
        }
        if (bitmaps.size() == 2 || (bitmaps.size() == 6 )){
            if (bitmaps.get(1) == null) {
                mPhotoView2.setImageDrawable(null);
            } else {
                mPhotoView2.setImageBitmap(bitmaps.get(1));
            }
        }
        if ((bitmaps.size() == 3) || (bitmaps.size() == 7 )) {
            if (bitmaps.get(2) == null) {
                mPhotoView3.setImageDrawable(null);
            } else {
                mPhotoView3.setImageBitmap(bitmaps.get(2));
            }
        }
        if ((bitmaps.size() == 4) || (bitmaps.size() == 8 )) {
            if (bitmaps.get(3) == null) {
                mPhotoView4.setImageDrawable(null);
            } else {
                mPhotoView4.setImageBitmap(bitmaps.get(3));
            }
        }


    }
    public void saveArrayList(ArrayList<String> list, String key){
        shared = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = shared.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(key, json);
        editor.apply();     // This line is IMPORTANT !!!
    }

    public ArrayList<String> getArrayList(String key){
        shared = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Gson gson = new Gson();
        String json = shared.getString(key, null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        return gson.fromJson(json, type);
    }



}


