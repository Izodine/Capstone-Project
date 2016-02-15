package com.syncedsoftware.iassembly;

import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.syncedsoftware.iassembly.iasm_base.Simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends ActionBarActivity implements SimulationLink {

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private SimulationTaskFragment mSimulationTaskFragment;
    private static final String TAG_SIMULATION_FRAGMENT = "SIMULATION_FRAGMENT";
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private TutorialLink mTutorialLink;

    AnalyticsApplication application;
    Tracker mTracker;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
        FragmentManager fm = getSupportFragmentManager();

        mSimulationTaskFragment = (SimulationTaskFragment) fm.findFragmentByTag(TAG_SIMULATION_FRAGMENT);

        if (mSimulationTaskFragment == null) {
            mSimulationTaskFragment = new SimulationTaskFragment();
            fm.beginTransaction().add(mSimulationTaskFragment, TAG_SIMULATION_FRAGMENT).commit();
        }

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mTracker.setScreenName("Page: " + mSectionsPagerAdapter.getPageTitle(position));
                mTracker.send(new HitBuilders.ScreenViewBuilder().build());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

    }

    public void initTutorial(int tutorialId){
        changePage(0);
        mTutorialLink.setTutorial(tutorialId);
    }

//    public void initFreeMode(){
//        setFreeMode();
//    }
//
//    public void setFreeMode(){
//        getCodeFragment().setIsTutorialMode(false);
//        FragmentManager manager = getSupportFragmentManager();
//        final CodeFooterFragment footerFragment = new CodeFooterFragment();
//        Bundle args = new Bundle();
//        args.putBoolean("TUTORIAL_MODE", false);
//        footerFragment.setArguments(args);
//        manager.beginTransaction().replace(R.id.code_footer_container,footerFragment).commit();
//        getCodeFragment().linkToFooter(footerFragment.getView(), this);
//    }
//
//    public void setTutorialMode(int lessonId){
//        getCodeFragment().setIsTutorialMode(true);
//        FragmentManager manager = getSupportFragmentManager();
//        final CodeFooterFragment footerFragment = new CodeFooterFragment();
//
//        // Set up footer for tutorial mode. Load the corresponding lesson text.
//        Bundle args = new Bundle();
//        args.putBoolean("TUTORIAL_MODE", true);
//        args.putInt(LESSON_TEXT_ID, lessonId);
//        footerFragment.setArguments(args);
//
//        // Setup code fragment for tutorial mode
//        manager.beginTransaction().replace(R.id.code_footer_container,footerFragment).commit();
//        getCodeFragment().linkToFooter(getCodeFragment().getView(), this);
//        getCodeFragment().setEditorCode(lessonId);
//    }

    public void changePage(int page){
        mViewPager.setCurrentItem(page);
    }

    @Override
    public void addSimulationListener(Simulation.SimulationListener listener) {
        mSimulationTaskFragment.addSimulationListener(listener);
    }

    @Override
    public void removeSimulationListener(Simulation.SimulationListener listener) {
        mSimulationTaskFragment.removeSimulationListener(listener);
    }

    @Override
    public void addSimulationReadyListener(Simulation.SimulationReadyListener listener) {
        mSimulationTaskFragment.addReadyListener(listener);
    }

    @Override
    public void removeSimulationReadyListener(Simulation.SimulationReadyListener listener) {
        mSimulationTaskFragment.removeReadyListener(listener);
    }

    @Override
    public void startSimulation(ArrayList<String> lines, int mode, Handler handler) {
        mSimulationTaskFragment.startSimulation(lines, mode, handler);
    }

    @Override
    public void stopSimulation() {
        mSimulationTaskFragment.stopSimulation();
    }

    @Override
    public void stepSimulation() {
        mSimulationTaskFragment.stepSimulation();
    }

//    public void stopSimulation(){
//        mSimulationTaskFragment.stopSimulation();
//        getCodeFragment().exitStepSimulation();
//        new AlertDialog.Builder(this)
//                .setTitle("Step Simulation Ended.")
//                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                })
//                .setIcon(android.R.drawable.ic_dialog_info)
//                .show();
//    }


    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        volatile Map<Integer, Fragment> fragmentMap = new HashMap<>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public Fragment getFragment(int index){
            return fragmentMap.get(index);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            fragmentMap.remove(position);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;
            switch(position){
                case 0:
                    fragment = new CodeFragment();
                    mTutorialLink = (CodeFragment)fragment;
                    break;
                case 1:
                    fragment = new RegistersFragment();
                    break;
                case 2:
                    fragment = new MemoryFragment();
                    break;
                case 3:
                    fragment = new DocsFragment();
                    break;
            }
            fragmentMap.put(position,fragment);
            return fragment;
        }

        @Override
        public int getCount(){
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.section_code_label);
                case 1:
                    return getString(R.string.section_registers_label);
                case 2:
                    return getString(R.string.section_memory_label);
                case 3:
                    return getString(R.string.section_docs_label);
            }
            return null;
        }
    }
}
