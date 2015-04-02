/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.studyjam.sunshine.app.data;

import com.studyjam.sunshine.app.data.WeatherContract.LocationEntry;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();

    // Since we want each test to start with a clean slate
    void deleteTheDatabase() {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
    }

    /*
        This function gets called before each test is executed to delete the database.  This makes
        sure that we always have a clean test.
     */
    public void setUp() {
        deleteTheDatabase();
    }

    /*
        Students: Uncomment this test once you've written the code to create the Location
        table.  Note that you will have to have chosen the same column names that I did in
        my solution for this test to compile, so if you haven't yet done that, this is
        a good time to change your column names to match mine.

        Note that this only tests that the Location table has the correct columns, since we
        give you the code for the weather table.  This test does not look at the
     */
    public void testCreateDb() throws Throwable {
        // build a HashSet of all of the table names we wish to look for
        // Note that there will be another table in the DB that stores the
        // Android metadata (db version information)
        final HashSet<String> tableNameHashSet = new HashSet<String>();
        tableNameHashSet.add(WeatherContract.LocationEntry.TABLE_NAME);
        tableNameHashSet.add(WeatherContract.WeatherEntry.TABLE_NAME);

        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new WeatherDbHelper(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());

        // have we created the tables we want?
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());

        // verify that the tables have been created
        do {
            tableNameHashSet.remove(c.getString(0));
        } while( c.moveToNext() );

        // if this fails, it means that your database doesn't contain both the location entry
        // and weather entry tables
        assertTrue("Error: Your database was created without both the location entry and weather entry tables",
                tableNameHashSet.isEmpty());

        // now, do our tables contain the correct columns?
        c = db.rawQuery("PRAGMA table_info(" + WeatherContract.LocationEntry.TABLE_NAME + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for
        final HashSet<String> locationColumnHashSet = new HashSet<String>();
        locationColumnHashSet.add(WeatherContract.LocationEntry._ID);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_CITY_NAME);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LAT);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LONG);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            locationColumnHashSet.remove(columnName);
        } while(c.moveToNext());

        // if this fails, it means that your database doesn't contain all of the required location
        // entry columns
        assertTrue("Error: The database doesn't contain all of the required location entry columns",
                locationColumnHashSet.isEmpty());
        db.close();
    }

    /*
        Students:  Here is where you will build code to test that we can insert and query the
        location database.  We've done a lot of work for you.  You'll want to look in TestUtilities
        where you can uncomment out the "createNorthPoleLocationValues" function.  You can
        also make use of the ValidateCurrentRecord function from within TestUtilities.
    */
    public void testLocationTable() {
        // First step: Get reference to writable database
        SQLiteDatabase database = new WeatherDbHelper(this.mContext).getWritableDatabase();
        // Create ContentValues of what you want to insert
        ContentValues locationValues = TestUtilities.createNorthPoleLocationValues();
        // Insert ContentValues into database and get a row ID back
        insertLocation(database, locationValues);
        // Query the database and receive a Cursor back
        Cursor cursor = database.query(LocationEntry.TABLE_NAME, null, null, null, null, null, null);
        // Move the cursor to a valid database row
        assertTrue("No valid rows in cursor", cursor.moveToFirst());
        // Validate data in resulting Cursor with the original ContentValues
        // (you can use the validateCurrentRecord function in TestUtilities to validate the
        // query if you like)
        Set<Map.Entry<String, Object>> set = locationValues.valueSet();
        for(Map.Entry<String, Object> entry : set){
            String key = entry.getKey();

            String expectedValue = entry.getValue().toString();

            int columnIndex = cursor.getColumnIndex(key);
            assertTrue("Column " + key + " doesn't exists", columnIndex != -1);
            String currentValue = cursor.getString(columnIndex);
            assertEquals(
                    "Values doesn't match in column "+ key,
                    currentValue,
                    expectedValue
                    );
        }
        // Finally, close the cursor and database
        cursor.close();
        database.close();

    }

    /*
        Students:  Here is where you will build code to test that we can insert and query the
        database.  We've done a lot of work for you.  You'll want to look in TestUtilities
        where you can use the "createWeatherValues" function.  You can
        also make use of the validateCurrentRecord function from within TestUtilities.
     */
    public void testWeatherTable() {
        // First step: Get reference to writable database
        SQLiteDatabase database = new WeatherDbHelper(this.mContext).getWritableDatabase();
        // Insert the location, and then use the locationRowId to insert
        // the weather. Make sure to cover as many failure cases as you can.
        long locationId = insertLocation(database, TestUtilities.createNorthPoleLocationValues());
        // Instead of rewriting all of the code we've already written in testLocationTable
        // we can move this code to insertLocation and then call insertLocation from both
        // tests. Why move it? We need the code to return the ID of the inserted location
        // and our testLocationTable can only return void because it's a test.


        // Create ContentValues of what you want to insert
        // (you can use the createWeatherValues TestUtilities function if you wish)
        ContentValues weatherValues = TestUtilities.createWeatherValues(locationId);
        // Insert ContentValues into database and get a row ID back
        database.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, weatherValues);
        // Query the database and receive a Cursor back
        Cursor cursor = database.query(WeatherContract.WeatherEntry.TABLE_NAME, null, null, null, null, null, null);
        // Move the cursor to a valid database row
        assertTrue("No valid rows in cursor", cursor.moveToFirst());
        // Validate data in resulting Cursor with the original ContentValues
        // (you can use the validateCurrentRecord function in TestUtilities to validate the
        // query if you like)
        Set<Map.Entry<String, Object>> set = weatherValues.valueSet();
        for(Map.Entry<String, Object> entry : set){
            String key = entry.getKey();

            String expectedValue = entry.getValue().toString();

            int columnIndex = cursor.getColumnIndex(key);
            assertTrue("Column " + key + " doesn't exists", columnIndex != -1);
            String currentValue = cursor.getString(columnIndex);
            assertEquals(
                    "Values doesn't match in column "+ key,
                    currentValue,
                    expectedValue
            );
        }
        // Finally, close the cursor and database
    }


    /*
        Students: This is a helper method for the testWeatherTable quiz. You can move your
        code from testLocationTable to here so that you can call this code from both
        testWeatherTable and testLocationTable.
     */
    public long insertLocation(SQLiteDatabase database, ContentValues locationValues) {
        assertTrue("Database is not opened", database.isOpen());
        long rowId = database.insert(WeatherContract.LocationEntry.TABLE_NAME, null, locationValues);
        assertTrue("The row was not insert", rowId != -1);
        return rowId;
    }
}