package it.collideorscopeapps.codename_hippopotamos.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.LiveFolders;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import it.collideorscopeapps.codename_hippopotamos.model.Playlist;
import it.collideorscopeapps.codename_hippopotamos.model.Quote;
import it.collideorscopeapps.codename_hippopotamos.model.Schermata;

public class QuotesProvider {

    //DB Manager notes:
    // TODO for the audio files, not stored into the db, check how to have them in the sd card only
    // i.e. by downloading them
    // TODO check if there is a default "app data" dir in the sd card, or path conventions
    // TODO make unit/integration tests

    // TODO check how to handle/synch/check db version here and in the db created from sql file
    // TODO

    //FIXME this failed on device, " unknown error (code 14): Could not open database"
    // SQLiteDatabase db = tryOpenDB(path,cf,flags);
    // seems fixed now, apparently was because of the static methods creating the db

    //TODO check if close causes errors if reopening

    // ensureDBOpen
    //TODO check the need for this, add tests. was giving error
    // when db closed
    // when db file manually deleted before runnning tests

    //End of DB Manager notes

    public static final int DATABASE_VERSION = 3;
    public static final String DB_NAME = "greekquotes";
    public static final String TAG = "QuotesProvider";

    public final Languages DEFAULT_LANGUAGE = Languages.EN;
    public enum Languages {
        //TODO check if numeric values cna be explicity assigned
        //TODO to ensure they matche the db
        NO_LANGUAGE, // = 0
        EN, // = 1
        IT // = 2
    }

    private static class DBHelper extends SQLiteOpenHelper {

        private static final String TAG = "DBHelper";
        private final Context myContext; //TODO check if final is necessary

        DBHelper(Context context) {//todo, should constructor by public?
            super(context, DB_NAME, null, DATABASE_VERSION);

            this.myContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            Log.d(TAG, "onCreate");

            AssetManager assetManager = myContext.getAssets();
            ArrayList<String> schemaStatements = Utils.getSchemaCreationStatementsFromSqlFile(assetManager);
            TreeMap<Integer,String> dataInsertStatements = Utils.getSingleLineSqlStatementsFromInputStream(
                    assetManager, Utils.DATA_INSERT_SQL_FILE);

            db.setForeignKeyConstraintsEnabled(true);
            execSchemaCreationQueries(db, schemaStatements);
            Log.d(TAG, "schema created");

            for(int i=0;i<dataInsertStatements.size();i++) {
                String statement = dataInsertStatements.get(i);
                try {
                    db.execSQL(statement);
                } catch (Exception e) {
                    Log.e(TAG, statement);
                    Log.e(TAG, e.toString());
                    throw e;
                }
            }
            Log.d(TAG, "data inserted");
        }

        private static void execSchemaCreationQueries(SQLiteDatabase myDatabase,
                                                     ArrayList<String> schemaStatements) {
            for (String statement : schemaStatements) {
                //Log.v(TAG,statement);
                myDatabase.execSQL(statement);
                //Log.v(TAG,getConcatTableNames(myDatabase));
            }
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);

            Log.d(TAG, "DB has been opened");

            //TODO get data from DB
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion);

            dropTables(db, myContext);
            onCreate(db);
        }

        public void dropTables(SQLiteDatabase db, Context context) {

            Log.w(TAG,", dropping DB tables..");

                    AssetManager assetManager = context.getAssets();
            TreeMap<Integer,String> dropSchemaStatements
                    = Utils.getSingleLineSqlStatementsFromInputStream(
                    assetManager,
                    Utils.DROP_SCHEMA_SQL_FILE);

            for(int i=0;i<dropSchemaStatements.size();i++) {
                String statement = dropSchemaStatements.get(i);
                try {
                    db.execSQL(statement);
                } catch (Exception e) {
                    Log.e(TAG, statement);
                    Log.e(TAG, e.toString());
                    throw e;
                }
            }
            Log.d(TAG, "Dropped DB schema and data.");
        }
    }

    private DBHelper mOpenHelper;
    private TreeMap<Integer, Schermata> schermate;
    public TreeMap<Integer,Playlist> getPlaylists() {
        return playlists;
    }
    private TreeMap<Integer,Playlist> playlists;

    public void create(Context context) {
        //TODO: ensure schema creation is not lenghty operation or make it asynch
        //only called from the application main thread,
        // and must avoid performing lengthy operations.
        // See the method descriptions for their expected thread behavior.
        mOpenHelper = new DBHelper(context);
    }

    /*public boolean onCreate() {
        //mOpenHelper = new DBHelper(getContext());
        return false;
    }*/

    public TreeMap<Integer, Schermata> getSchermateById(QuotesProvider.Languages language) {

        if(this.schermate != null) {
            return this.schermate;
        }

        // todo, translations languages
        // TODO some fields use a default language if the preferred one is absent


        //myCreateDBFromSqlFile();
        //openDatabaseReadonly();

        TreeMap<Integer, Schermata> newSchermate = new TreeMap<Integer, Schermata>();
        TreeMap<Integer, Quote> newAllQuotes = new TreeMap<Integer, Quote>();

        TreeMap<Integer, String> linguisticNotes = getLinguisticNotes(language);
        TreeMap<Integer, String> easterEggComments = getEasterEggComments(language);
        this.playlists = getPlaylistsFromDB();

        // TODO rededish, review: problems with db creation,
        //  opening, state, and closing cycles
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        processGetAllQuotesQuery(db, newAllQuotes);
        processScreensAndQuotesQuery(db, newSchermate, linguisticNotes, easterEggComments);
        setShortAndFullQuotesInScreens(newAllQuotes, newSchermate);

        this.schermate = newSchermate;
        for(Playlist pl :this.playlists.values()) {
            pl.setSchermate(this.schermate);
        }

        //FIXME removing this db.close();//not needed anymore once data is loaded
        return this.schermate;
    }


    private static void setShortAndFullQuotesInScreens(
            TreeMap<Integer, Quote> allQuotes,
            TreeMap<Integer, Schermata> allScreens) {

        for(Schermata screen: allScreens.values()) {
            if(null != screen.getShortQuoteId()) {
                Quote shortQuote = allQuotes.get(screen.getShortQuoteId());
                screen.setShortQuote(shortQuote);
            }
            if(null != screen.getFullQuoteId()) {
                Quote fullQuote = allQuotes.get(screen.getFullQuoteId());
                screen.setFulltQuote(fullQuote);
            }
        }
    }

    private static void processScreensAndQuotesQuery(
            SQLiteDatabase db,
            TreeMap<Integer, Schermata> newSchermate,
            TreeMap<Integer, String> linguisticNotes,
            TreeMap<Integer, String> easterEggComments) {
        String quotesAndSchermateQuery = "SELECT * FROM v_schermate_and_quotes";
        try(Cursor cursor = db.rawQuery(quotesAndSchermateQuery, null)) {
            // todo merge try

            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {
                addQuoteAndSchermata(cursor,
                        newSchermate,
                        linguisticNotes,
                        easterEggComments);
                cursor.moveToNext();
            }
        }
        catch (SQLiteException sqle) {
            Log.e(TAG, sqle.toString());
            Log.e(TAG, "Tables: " + getConcatTableNames(db));
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private static void processGetAllQuotesQuery(
            SQLiteDatabase db,
            TreeMap<Integer, Quote> allQuotes
    ) {
        String quotesAndSchermateQuery = "SELECT * FROM greek_quotes";
        try(Cursor cursor = db.rawQuery(quotesAndSchermateQuery, null)) { // todo merge try

            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {
                addQuote(cursor, allQuotes);
                cursor.moveToNext();
            }
        }
    }

    private static String getConcatTableNames(SQLiteDatabase db) {

        String allTableNamesQuery = "SELECT name " +
                "FROM sqlite_master " +
                "WHERE type='table' OR type = 'view' ";
        //ArrayList<String> tableNames = new ArrayList<>();
        String tableNamesConcat = "";
        int tablesCount = 0;
        Cursor c = db.rawQuery(allTableNamesQuery, null);
        if (c.moveToFirst()) {
            while ( !c.isAfterLast() ) {
                int firstColumnIdx = 0;
                String tableName = c.getString(firstColumnIdx);
                //tableNames.add(tableName);
                tableNamesConcat += tableName + ",";
                tablesCount++;
                c.moveToNext();
            }
        }
        Log.v(TAG,"Tables in DB ("
                + tablesCount + ") " + tableNamesConcat);

        //if(tablesCount < 3) {isDBEmpty = true;}

        return tableNamesConcat;
    }

    private static void addQuote(Cursor cursor,
                                 TreeMap<Integer, Quote> allQuotes) {

        final int greekQuoteIddColIdx = cursor.getColumnIndex("_id");
        final int quoteColIdx = cursor.getColumnIndex("quoteText");
        final int phoneticColIdx = cursor.getColumnIndex("phoneticTranscription");
        final int audioFileNameColIdx = cursor.getColumnIndex("audioFileName");

        int idQuote = cursor.getInt(greekQuoteIddColIdx);
        String greekQuote = cursor.getString(quoteColIdx);
        String phoneticTranscription = cursor.getString(phoneticColIdx);
        String audioFileName = cursor.getString(audioFileNameColIdx);

        Quote currentQuote = new Quote(idQuote, greekQuote,
                phoneticTranscription, audioFileName);
        allQuotes.put(idQuote, currentQuote);
    }

    private static void addQuoteAndSchermata(Cursor cursor,
                                             TreeMap<Integer,Schermata> schermate,
                                             TreeMap<Integer,String> linguisticNotes,
                                             TreeMap<Integer,String> easterEggComments) {

        final int schermataIdColIdx = cursor.getColumnIndex("s_id");
        final int greekQuoteIddColIdx = cursor.getColumnIndex("gq_id");
        final int quoteColIdx = cursor.getColumnIndex("quote");
        final int short_quote_idColIdx = cursor.getColumnIndex("short_quote_id");
        final int full_quote_idColIdx = cursor.getColumnIndex("full_quote_id");
        final int phoneticColIdx = cursor.getColumnIndex("phoneticTranscription");
        final int positionColIdx = cursor.getColumnIndex("position");
        final int titleColIdx = cursor.getColumnIndex("title");
        final int descriptionColIdx = cursor.getColumnIndex("description");
        final int defaultTranslationColIdx = cursor.getColumnIndex("default_translation");
        final int citColIdx = cursor.getColumnIndex("cit");
        final int audioFileNameColIdx = cursor.getColumnIndex("audioFileName");

        int idSchermata = cursor.getInt(schermataIdColIdx);
        int idQuote = cursor.getInt(greekQuoteIddColIdx);
        Integer shortQuoteId = getNullableInteger(cursor, short_quote_idColIdx);
        Integer fullQuoteId = getNullableInteger(cursor, full_quote_idColIdx);
        String greekQuote = cursor.getString(quoteColIdx);
        String phoneticTranscription = cursor.getString(phoneticColIdx);
        int quotePosition = cursor.getInt(positionColIdx);
        String title = cursor.getString(titleColIdx);
        String description = cursor.getString(descriptionColIdx);
        String cit = cursor.getString(citColIdx);
        String audioFileName = cursor.getString(audioFileNameColIdx);

        String linguisticNote = linguisticNotes.get(idSchermata);
        String eeComment = easterEggComments.get(idSchermata);

        // NB schermata translation is used in place of those of each quote
        //TODO change this on the basis of user language preferences
        String defaultTranslation = cursor.getString(defaultTranslationColIdx);
        //Log.v(TAG,"Translation: " + defaultTranslation);

        Quote currentQuote = new Quote(idQuote, quotePosition, greekQuote,
                phoneticTranscription, audioFileName);

        Schermata currentSchermata = schermate.get(idSchermata);
        if(null == currentSchermata) {

            currentSchermata= new Schermata(
                    idSchermata,
                    title,
                    description,
                    shortQuoteId,
                    fullQuoteId,
                    defaultTranslation,
                    linguisticNote,
                    cit,
                    eeComment);
            schermate.put(idSchermata, currentSchermata);
        }
        currentSchermata.addQuote(currentQuote);
    }

    private static Integer getNullableInteger(Cursor cursor, int colIdx) {

        Integer value = null;
        boolean colIsNull = Cursor.FIELD_TYPE_NULL == cursor.getType(colIdx);
        if(!colIsNull) {
            value = cursor.getInt(colIdx);
        }

        return value;
    }

    private static Integer getNullableInteger(Cursor cursor, String colName) {

        return getNullableInteger(cursor, cursor.getColumnIndex(colName));
    }

    private TreeMap<Integer, Playlist> getPlaylistsFromDB() {

        TreeMap<Integer,Playlist> playlists = new TreeMap<>();

        final String PLAYLISTS_T = "v_playlists";

        try(SQLiteDatabase db = mOpenHelper.getReadableDatabase()) {

            int playlistCount = (int) DatabaseUtils.queryNumEntries(
                    db,PLAYLISTS_T,null);

            String playlistsQuery = "SELECT * FROM " + PLAYLISTS_T;
            Cursor cursor = db.rawQuery(playlistsQuery, null);
            cursor.moveToFirst();

            while(!cursor.isAfterLast()) {

                Integer playlistId = cursor.getInt(cursor.getColumnIndex("p_id"));
                String description = cursor.getString(cursor.getColumnIndex("description"));

                int playlistRank = getPlaylistRank(cursor,playlistCount,playlistId);

                int disabledAsInt = cursor.getInt(cursor.getColumnIndex("disabled"));
                boolean disabled = Utils.castSqliteBoolean(disabledAsInt);
                String schermateConcat = cursor.getString(cursor.getColumnIndex("schermate"));
                String playOrderConcat = cursor.getString(cursor.getColumnIndex("sorting"));

                TreeSet<Integer> schermateIds = Utils.getIntsFromConcatString(schermateConcat);
                TreeSet<Integer> playOrderRanks = Utils.getIntsFromConcatString(playOrderConcat);

                TreeMap<Integer, Integer> playListAsRankedSchermate = new TreeMap<>();
                Iterator<Integer> schermateIdsItr = schermateIds.iterator();
                Iterator<Integer> playOrderRanksItr = playOrderRanks.iterator();
                while(schermateIdsItr.hasNext()) {
                    int key = schermateIdsItr.next();
                    int value = playOrderRanksItr.next();

                    playListAsRankedSchermate.put(key, value);
                }

                Playlist currentPlaylist = new Playlist(description,
                        playListAsRankedSchermate,
                        disabled);

                playlists.put(playlistRank,currentPlaylist);
                cursor.moveToNext();
            }
        }

        return playlists;
    }

    private static int getPlaylistRank(Cursor cursor, int playlistsCount, int playlistId) {

        Integer rankInDB = getNullableInteger(cursor,"playlist_rank");
        int rank;

        if(rankInDB == null) {
            rank = playlistsCount + playlistId;
        } else {
            rank = rankInDB;
        }
        return rank;
    }

    public  TreeMap<Integer, String> getEasterEggComments(QuotesProvider.Languages language) {

        // TODO use eeComment in the default language when it's the only one

        TreeMap<Integer, String> eeComments = new TreeMap<Integer, String>();

        try(SQLiteDatabase db = mOpenHelper.getReadableDatabase()) {

            String eeCommentsQuery = "SELECT * FROM easter_egg_comments WHERE language_id = "
                    + language.ordinal();
            Cursor cursor = db.rawQuery(eeCommentsQuery, null);

            cursor.moveToFirst();

            while(!cursor.isAfterLast()) {

                Integer schermataId = cursor.getInt(cursor.getColumnIndex("schermata_id"));
                String eeComment = cursor.getString(cursor.getColumnIndex("eeComment"));
                eeComments.put(schermataId, eeComment);

                cursor.moveToNext();
            }
        }

        return eeComments;
    }

    public TreeMap<Integer, String> getLinguisticNotes(QuotesProvider.Languages language) {

        TreeMap<Integer, String> linguisticNotes = new TreeMap<Integer, String>();

        try(SQLiteDatabase db = mOpenHelper.getReadableDatabase()) {

            String linguisticNotesQuery = "SELECT * FROM linguistic_notes WHERE language_id = "
                    + language.ordinal();
            Cursor cursor = db.rawQuery(linguisticNotesQuery, null);

            cursor.moveToFirst();

            while(!cursor.isAfterLast()) {

                Integer schermataId = cursor.getInt(cursor.getColumnIndex("schermata_id"));
                String linguisticNote = cursor.getString(cursor.getColumnIndex("linguisticNote"));

                linguisticNotes.put(schermataId, linguisticNote);

                cursor.moveToNext();
            }
        }
        return linguisticNotes;
    }

    public boolean isDBEmpty(Context myContext, SQLiteDatabase db) {

        Boolean isDBEmpty = true;
        //..removed stuff ensureDBOpen
        String tableNamesConcat = getConcatTableNames(db);
        isDBEmpty = !tableNamesConcat.contains("v_schermate_and_quotes,");
        return isDBEmpty;
    }

    public synchronized void close() {
        //super.close();
        mOpenHelper.close();
    }
}