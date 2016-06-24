package net.mzimmer.android.apps.smartpad;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

interface OnTextChangeListener {
	void onTextChange(TextView view, String text);

	class Helper {
		public static void register(final TextView view, final OnTextChangeListener listener) {
			view.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					listener.onTextChange(view, s.toString());
				}
			});
		}
	}
}
