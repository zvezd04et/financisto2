/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto2.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ListAdapter;
import android.widget.TabHost;
import android.widget.Toast;

import ru.orangesoftware.financisto2.R;
import ru.orangesoftware.financisto2.db.DatabaseAdapter;
import ru.orangesoftware.financisto2.db.DatabaseAdapter_;
import ru.orangesoftware.financisto2.export.csv.CsvExportOptions;
import ru.orangesoftware.financisto2.export.csv.CsvExportTask;
import ru.orangesoftware.financisto2.export.csv.CsvImportOptions;
import ru.orangesoftware.financisto2.export.csv.CsvImportTask;
import ru.orangesoftware.financisto2.export.qif.QifExportOptions;
import ru.orangesoftware.financisto2.export.qif.QifExportTask;
import ru.orangesoftware.financisto2.export.qif.QifImportOptions;
import ru.orangesoftware.financisto2.export.qif.QifImportTask;
import ru.orangesoftware.financisto2.utils.EntityEnum;
import ru.orangesoftware.financisto2.utils.EnumUtils;
import ru.orangesoftware.financisto2.utils.ExecutableEntityEnum;
import ru.orangesoftware.financisto2.utils.IntegrityFix;
import ru.orangesoftware.financisto2.utils.MyPreferences;

import static ru.orangesoftware.financisto2.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import static ru.orangesoftware.financisto2.utils.EnumUtils.showPickOneDialog;

public class MainActivity extends TabActivity implements TabHost.OnTabChangeListener {

    private static final int ACTIVITY_CSV_EXPORT = 2;
    private static final int ACTIVITY_QIF_EXPORT = 3;
    private static final int ACTIVITY_CSV_IMPORT = 4;
    private static final int ACTIVITY_QIF_IMPORT = 5;
    private static final int CHANGE_PREFERENCES = 6;

    private static final int MENU_PREFERENCES = Menu.FIRST + 1;
    private static final int MENU_ABOUT = Menu.FIRST + 2;
    private static final int MENU_BACKUP = Menu.FIRST + 3;
    private static final int MENU_RESTORE = Menu.FIRST + 4;
    private static final int MENU_SCHEDULED_TRANSACTIONS = Menu.FIRST + 5;
    private static final int MENU_ENTITIES = Menu.FIRST + 8;
    private static final int MENU_MASS_OP = Menu.FIRST + 9;
    private static final int MENU_DONATE = Menu.FIRST + 10;
    private static final int MENU_IMPORT_EXPORT = Menu.FIRST + 11;
    private static final int MENU_BACKUP_TO = Menu.FIRST + 12;
    private static final int MENU_INTEGRITY_FIX = Menu.FIRST + 13;
    private static final int MENU_PLANNER = Menu.FIRST + 14;
    private static final int MENU_BACKUP_RESTORE_ONLINE = Menu.FIRST + 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        final TabHost tabHost = getTabHost();

        setupAccountsTab(tabHost);
        setupBlotterTab(tabHost);
        setupBudgetsTab(tabHost);
        setupReportsTab(tabHost);

        MyPreferences.StartupScreen screen = MyPreferences.getStartupScreen(this);
        tabHost.setCurrentTabByTag(screen.tag);

        tabHost.setOnTabChangedListener(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_CSV_EXPORT) {
            if (resultCode == RESULT_OK) {
                CsvExportOptions options = CsvExportOptions.fromIntent(data);
                doCsvExport(options);
            }
        } else if (requestCode == ACTIVITY_QIF_EXPORT) {
            if (resultCode == RESULT_OK) {
                QifExportOptions options = QifExportOptions.fromIntent(data);
                doQifExport(options);
            }
        } else if (requestCode == ACTIVITY_CSV_IMPORT) {
            if (resultCode == RESULT_OK) {
                CsvImportOptions options = CsvImportOptions.fromIntent(data);
                doCsvImport(options);
            }
        } else if (requestCode == ACTIVITY_QIF_IMPORT) {
            if (resultCode == RESULT_OK) {
                QifImportOptions options = QifImportOptions.fromIntent(data);
                doQifImport(options);
            }
        } else if (requestCode == CHANGE_PREFERENCES) {
            scheduleNextAutoBackup(this);
        }
    }

    @Override
    public void onTabChanged(String tabId) {
        Log.d("Financisto", "About to update tab " + tabId);
        long t0 = System.currentTimeMillis();
        refreshCurrentTab();
        long t1 = System.currentTimeMillis();
        Log.d("Financisto", "Tab " + tabId + " updated in " + (t1 - t0) + "ms");
    }

    private void refreshCurrentTab() {
        Context c = getTabHost().getCurrentView().getContext();
        if (c instanceof RefreshSupportedActivity) {
            RefreshSupportedActivity activity = (RefreshSupportedActivity) c;
            activity.recreateCursor();
            activity.integrityCheck();
        }
    }

    private void setupAccountsTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("accounts")
                .setIndicator(getString(R.string.accounts), getResources().getDrawable(R.drawable.ic_tab_accounts))
                .setContent(new Intent(this, AccountListActivity.class)));
    }

    private void setupBlotterTab(TabHost tabHost) {
        Intent intent = new Intent(this, BlotterActivity.class);
        intent.putExtra(BlotterActivity.SAVE_FILTER, true);
        intent.putExtra(BlotterActivity.EXTRA_FILTER_ACCOUNTS, true);
        tabHost.addTab(tabHost.newTabSpec("blotter")
                .setIndicator(getString(R.string.blotter), getResources().getDrawable(R.drawable.ic_tab_blotter))
                .setContent(intent));
    }

    private void setupBudgetsTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("budgets")
                .setIndicator(getString(R.string.budgets), getResources().getDrawable(R.drawable.ic_tab_budgets))
                .setContent(new Intent(this, BudgetListActivity.class)));
    }

    private void setupReportsTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("reports")
                .setIndicator(getString(R.string.reports), getResources().getDrawable(R.drawable.ic_tab_reports))
                .setContent(new Intent(this, ReportsListActivity.class)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem menuItem = menu.add(0, MENU_ENTITIES, 0, R.string.entities);
        menuItem.setIcon(R.drawable.menu_entities);
        menuItem = menu.add(0, MENU_SCHEDULED_TRANSACTIONS, 0, R.string.scheduled_transactions);
        menuItem.setIcon(R.drawable.ic_menu_today);
        menuItem = menu.add(0, MENU_MASS_OP, 0, R.string.mass_operations);
        menuItem.setIcon(R.drawable.ic_menu_agenda);
        menuItem = menu.add(0, MENU_BACKUP, 0, R.string.backup_database);
        menuItem.setIcon(R.drawable.ic_menu_upload);
        menuItem.setIcon(android.R.drawable.ic_menu_preferences);
        menu.addSubMenu(0, MENU_PLANNER, 0, R.string.planner);
        menu.addSubMenu(0, MENU_PREFERENCES, 0, R.string.preferences);
        menu.addSubMenu(0, MENU_RESTORE, 0, R.string.restore_database);
        menu.addSubMenu(0, MENU_BACKUP_RESTORE_ONLINE, 0, R.string.backup_restore_database_online);
        menu.addSubMenu(0, MENU_IMPORT_EXPORT, 0, R.string.import_export);
        menu.addSubMenu(0, MENU_BACKUP_TO, 0, R.string.backup_database_to);
        menu.addSubMenu(0, MENU_INTEGRITY_FIX, 0, R.string.integrity_fix);
        menu.addSubMenu(0, MENU_DONATE, 0, R.string.donate);
        menu.addSubMenu(0, MENU_ABOUT, 0, R.string.about);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case MENU_ENTITIES:
                final MenuEntities[] entities = MenuEntities.values();
                ListAdapter adapter = EnumUtils.createEntityEnumAdapter(this, entities);
                final AlertDialog d = new AlertDialog.Builder(this)
                        .setAdapter(adapter, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                MenuEntities e = entities[which];
                                startActivity(new Intent(MainActivity.this, e.getActivityClass()));
                            }
                        })
                        .create();
                d.setTitle(R.string.entities);
                d.show();
                break;
            case MENU_PREFERENCES:
                startActivityForResult(new Intent(this, PreferencesActivity.class), CHANGE_PREFERENCES);
                break;
            case MENU_SCHEDULED_TRANSACTIONS:
                startActivity(new Intent(this, ScheduledListActivity.class));
                break;
            case MENU_MASS_OP:
                startActivity(new Intent(this, MassOpActivity.class));
                break;
            case MENU_PLANNER:
                startActivity(new Intent(this, PlannerActivity.class));
                break;
            case MENU_ABOUT:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            case MENU_DONATE:
                openBrowser("market://search?q=pname:ru.orangesoftware.financisto2.support");
                break;
            case MENU_IMPORT_EXPORT:
                showPickOneDialog(this, R.string.import_export, ImportExportEntities.values(), this);
                break;
            case MENU_INTEGRITY_FIX:
                doIntegrityFix();
                break;
        }
        return false;
    }

    private void doIntegrityFix() {
        new IntegrityFixTask().execute();
    }

    private void openBrowser(String url) {
        try {
            Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception ex) {
            //eventually market is not available
            Toast.makeText(this, R.string.donate_error, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Treat asynchronous requests to popup error messages
     */
    private Handler handler = new Handler() {
        /**
         * Schedule the popup of the given error message
         * @param msg The message to display
         **/
        @Override
        public void handleMessage(Message msg) {
            showErrorPopup(MainActivity.this, msg.what);
        }
    };

    public void showErrorPopup(Context context, int message) {
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setTitle(R.string.error)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(true)
                .create()
                .show();
    }

    private void doCsvExport(CsvExportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.csv_export_inprogress), true);
        new CsvExportTask(this, progressDialog, options).execute();
    }

    private void doCsvImport(CsvImportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.csv_import_inprogress), true);
        new CsvImportTask(this, handler, progressDialog, options).execute();
    }

    private void doQifExport(QifExportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.qif_export_inprogress), true);
        new QifExportTask(this, progressDialog, options).execute();
    }

    private void doQifImport(QifImportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.qif_import_inprogress), true);
        new QifImportTask(this, handler, progressDialog, options).execute();
    }

    private void doCsvExport() {
        Intent intent = new Intent(this, CsvExportActivity.class);
        startActivityForResult(intent, ACTIVITY_CSV_EXPORT);
    }

    private void doCsvImport() {
        Intent intent = new Intent(this, CsvImportActivity.class);
        startActivityForResult(intent, ACTIVITY_CSV_IMPORT);
    }

    private void doQifExport() {
        Intent intent = new Intent(this, QifExportActivity.class);
        startActivityForResult(intent, ACTIVITY_QIF_EXPORT);
    }

    private void doQifImport() {
        Intent intent = new Intent(this, QifImportActivity.class);
        startActivityForResult(intent, ACTIVITY_QIF_IMPORT);
    }

    private enum MenuEntities implements EntityEnum {

        CURRENCIES(R.string.currencies, R.drawable.menu_entities_currencies, CurrencyListActivity.class),
        EXCHANGE_RATES(R.string.exchange_rates, R.drawable.menu_entities_exchange_rates, ExchangeRatesListActivity.class),
        CATEGORIES(R.string.categories, R.drawable.menu_entities_categories, CategoryListActivity2.class),
        PAYEES(R.string.payees, R.drawable.menu_entities_payees, PayeeListActivity.class),
        PROJECTS(R.string.projects, R.drawable.menu_entities_projects, ProjectListActivity.class);

        private final int titleId;
        private final int iconId;
        private final Class<?> actitivyClass;

        MenuEntities(int titleId, int iconId, Class<?> activityClass) {
            this.titleId = titleId;
            this.iconId = iconId;
            this.actitivyClass = activityClass;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

        @Override
        public int getIconId() {
            return iconId;
        }

        public Class<?> getActivityClass() {
            return actitivyClass;
        }

    }

    private enum ImportExportEntities implements ExecutableEntityEnum<MainActivity> {

        CSV_EXPORT(R.string.csv_export, R.drawable.ic_menu_back) {
            @Override
            public void execute(MainActivity mainActivity) {
                mainActivity.doCsvExport();
            }
        },
        CSV_IMPORT(R.string.csv_import, R.drawable.ic_menu_forward) {
            @Override
            public void execute(MainActivity mainActivity) {
                mainActivity.doCsvImport();
            }
        },
        QIF_EXPORT(R.string.qif_export, R.drawable.ic_menu_back) {
            @Override
            public void execute(MainActivity mainActivity) {
                mainActivity.doQifExport();
            }
        },
        QIF_IMPORT(R.string.qif_import, R.drawable.ic_menu_forward) {
            @Override
            public void execute(MainActivity mainActivity) {
                mainActivity.doQifImport();
            }
        };

        private final int titleId;
        private final int iconId;

        ImportExportEntities(int titleId, int iconId) {
            this.titleId = titleId;
            this.iconId = iconId;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

        @Override
        public int getIconId() {
            return iconId;
        }

    }

    private class IntegrityFixTask extends AsyncTask<Void, Void, Void> {

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, null, getString(R.string.integrity_fix_in_progress), true);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Void o) {
            refreshCurrentTab();
            progressDialog.dismiss();
        }

        @Override
        protected Void doInBackground(Void... objects) {
            DatabaseAdapter db = DatabaseAdapter_.getInstance_(MainActivity.this);
            new IntegrityFix(db).fix();
            return null;
        }
    }

}
