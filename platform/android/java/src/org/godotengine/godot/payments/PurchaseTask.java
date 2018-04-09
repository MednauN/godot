/*************************************************************************/
/*  PurchaseTask.java                                                    */
/*************************************************************************/
/*                       This file is part of:                           */
/*                           GODOT ENGINE                                */
/*                      https://godotengine.org                          */
/*************************************************************************/
/* Copyright (c) 2007-2018 Juan Linietsky, Ariel Manzur.                 */
/* Copyright (c) 2014-2018 Godot Engine contributors (cf. AUTHORS.md)    */
/*                                                                       */
/* Permission is hereby granted, free of charge, to any person obtaining */
/* a copy of this software and associated documentation files (the       */
/* "Software"), to deal in the Software without restriction, including   */
/* without limitation the rights to use, copy, modify, merge, publish,   */
/* distribute, sublicense, and/or sell copies of the Software, and to    */
/* permit persons to whom the Software is furnished to do so, subject to */
/* the following conditions:                                             */
/*                                                                       */
/* The above copyright notice and this permission notice shall be        */
/* included in all copies or substantial portions of the Software.       */
/*                                                                       */
/* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,       */
/* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF    */
/* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.*/
/* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY  */
/* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,  */
/* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE     */
/* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                */
/*************************************************************************/

package org.godotengine.godot.payments;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

abstract public class PurchaseTask {

	private Activity context;

	private InAppBillingServiceProvider mServiceProvider;
	public PurchaseTask(InAppBillingServiceProvider serviceProvider, Activity context) {
		this.context = context;
		this.mServiceProvider = serviceProvider;
	}

	private boolean isLooping = false;

	public void purchase(final String sku, final String transactionId) {
		Log.d("XXX", "Starting purchase for: " + sku);
		final PaymentsCache pc = new PaymentsCache(context);
		Boolean isBlocked = pc.getConsumableFlag("block", sku);
		/*
		if(isBlocked){
			Log.d("XXX", "Is awaiting payment confirmation");
			error("Awaiting payment confirmation");
			return;
		}
		*/
		final String hash = transactionId;

		new AsyncTask<Void, Void, Integer>() {

			String mErrorMessage = "Unknown error";

			@Override
			protected Integer doInBackground(Void... args) {
				Bundle buyIntentBundle;
				try {
					IInAppBillingService service = mServiceProvider.getBillingServiceWithTimeout();
					buyIntentBundle = service.getBuyIntent(3, context.getApplicationContext().getPackageName(), sku, "inapp", hash);

					Object rc = buyIntentBundle.get("RESPONSE_CODE");
					int responseCode = 0;
					if (rc == null) {
						responseCode = PaymentsManager.BILLING_RESPONSE_RESULT_OK;
					} else if (rc instanceof Integer) {
						responseCode = (Integer) rc;
					} else if (rc instanceof Long) {
						responseCode = (int)((Long)rc).longValue();
					}

					//Log.d("XXX", "Buy intent response code: " + responseCode);
					if (responseCode == 1 || responseCode == 3 || responseCode == 4 || responseCode == 7) {
						return responseCode;
					}

					PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
					pc.setConsumableValue("validation_hash", sku, hash);
					if (context == null) {
						//Log.d("XXX", "No context!");
					}
					if (pendingIntent == null) {
						//Log.d("XXX", "No pending intent");
					}
					//Log.d("XXX", "Starting activity for purchase!");
					context.startIntentSenderForResult(
							pendingIntent.getIntentSender(),
							PaymentsManager.REQUEST_CODE_FOR_PURCHASE,
							new Intent(),
							0, 0,
							0);

					return responseCode;
				} catch (Exception e) {
					//Log.d("XXX", "Error: " + e.getMessage());
					mErrorMessage = e.getMessage();
					return -1;
				}
			}

			@Override
			protected void onPostExecute(Integer responseCode) {
				if (responseCode == 1 || responseCode == 3 || responseCode == 4) {
					canceled();
					return;
				}
				if (responseCode == 7) {
					alreadyOwned();
					return;
				}
				if (responseCode != PaymentsManager.BILLING_RESPONSE_RESULT_OK) {
					error(mErrorMessage);
				}
			}

		}
			.execute();
	}

	abstract protected void error(String message);
	abstract protected void canceled();
	abstract protected void alreadyOwned();
}
