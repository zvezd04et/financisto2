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
package ru.orangesoftware.financisto2.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.View;

import ru.orangesoftware.financisto2.R;
import ru.orangesoftware.financisto2.db.DatabaseAdapter;

public class TemplateListAdapter extends BlotterListAdapter {

	public TemplateListAdapter(Context context, DatabaseAdapter db, Cursor c) {
		super(context);
        initWithCursor(c);
        initWithItemLayout(R.layout.template_list_item);
	}

    @Override
    protected boolean isShowRunningBalance() {
        return false;
    }

    @Override
	public void bindView(View view, Context context, Cursor cursor) {
		super.bindView(view, context, cursor);
	}

}
