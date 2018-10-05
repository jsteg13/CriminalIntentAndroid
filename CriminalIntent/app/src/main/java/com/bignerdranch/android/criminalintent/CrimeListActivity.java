package com.bignerdranch.android.criminalintent;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

public class CrimeListActivity extends SingleFragmentActivity implements CrimeListFragment.Callbacks, CrimeFragment.Callbacks {

    @Override
    protected Fragment createFragment() {
        return new CrimeListFragment();
    }

    @Override
    public void onCrimeUpdated(Crime crime) {
        if (findViewById(R.id.fragment_container) == null) {
            Intent intent = CrimePagerActivity.newIntent(this, crime.getId());
            startActivity(intent);
        } else {
            Fragment newDetail = CrimeFragment.newInstance(crime.getId());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, newDetail)
                    .commit();
        }
    }

//    @Override
//    public void onCrimeUpdated(Crime crime) {
//        CrimeListFragment listFragment = (CrimeListFragment)
//                getSupportFragmentManager()
//                        .findFragmentById(R.id.fragment_container);
//        listFragment.updateUI();
//    }




    private void updateSubtitle() {
        CrimeLab crimeLab = CrimeLab.get(getParent());
        int crimeCount = crimeLab.getCrimes().size();
        @SuppressLint("StringFormatMatches") String subtitle = getString(R.string.subtitle_format, crimeCount);

        AppCompatActivity activity = (AppCompatActivity) getParent();
        activity.getSupportActionBar().setSubtitle(subtitle);
    }

}
