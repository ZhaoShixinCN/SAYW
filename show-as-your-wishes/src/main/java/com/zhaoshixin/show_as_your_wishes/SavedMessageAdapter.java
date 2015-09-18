package com.zhaoshixin.show_as_your_wishes;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.List;
import com.fortysevendeg.swipelistview.SwipeListView;


public class SavedMessageAdapter extends BaseAdapter {

    private List<String> data;
    private Context context;
    private SwipeListView mView;
    private MySQLiteHelper mHelper;

    public SavedMessageAdapter(Context context, List<String> data, SwipeListView view, MySQLiteHelper helper) {
        this.context = context;
        this.data = data;
        this.mView = view;
        this.mHelper = helper;
    }

    public int add(String str) {
        data.add(str);
        notifyDataSetChanged();

        return 1;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public String getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final String item = getItem(position);
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = li.inflate(R.layout.saved_message, parent, false);
            holder = new ViewHolder();
            holder.tvTitle = (TextView) convertView.findViewById(R.id.row_message);
            holder.bAction1 = (Button) convertView.findViewById(R.id.action_modify_saved);
            holder.bAction3 = (Button) convertView.findViewById(R.id.action_delete_saved);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ((SwipeListView)parent).recycle(convertView, position);
        holder.tvTitle.setText(item);
        holder.bAction1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        holder.bAction3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获得数据库对象
                SQLiteDatabase db = mHelper.getWritableDatabase();
                //删除数据库中的该项
                String sql = "delete from normal_save_message where normalMessage='"+ data.get(position) + "'";
                db.execSQL(sql);
                db.close();//关闭数据库对象

                data.remove(position);
                notifyDataSetChanged();
            }
        });


        return convertView;
    }

    static class ViewHolder {
        TextView tvTitle;
        Button bAction1;
        Button bAction3;
    }
}
