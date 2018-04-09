package org.godotengine.godot.payments;

import com.android.vending.billing.IInAppBillingService;

import java.util.concurrent.TimeoutException;

interface InAppBillingServiceProvider {
	IInAppBillingService getBillingServiceWithTimeout() throws TimeoutException;
	IInAppBillingService getBillingService();
}
