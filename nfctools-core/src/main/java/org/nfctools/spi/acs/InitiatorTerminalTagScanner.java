/**
 * Copyright 2011-2012 Adrian Stabiszewski, as@nfctools.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nfctools.spi.acs;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;

import org.nfctools.api.TagListener;
import org.nfctools.api.TagType;
import org.nfctools.scio.TerminalStatus;
import org.nfctools.scio.TerminalStatusListener;

public class InitiatorTerminalTagScanner implements Runnable {

	private CardTerminal cardTerminal;
	private TerminalStatusListener statusListener;
	private TagListener tagListener;

	public InitiatorTerminalTagScanner(CardTerminal cardTerminal, TerminalStatusListener statusListener,
			TagListener tagListener) {
		this.cardTerminal = cardTerminal;
		this.statusListener = statusListener;
		this.tagListener = tagListener;
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			notifyStatus(TerminalStatus.WAITING);
			try {
				if (cardTerminal.waitForCardPresent(500)) {
					Card card = null;
					try {
						card = cardTerminal.connect("*");
						handleCard(card);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					finally {
						if (card != null) {
							card.disconnect(true);
						}
						try {
							while (cardTerminal.isCardPresent()) {
								try {
									Thread.sleep(500);
								}
								catch (InterruptedException e) {
									break;
								}
							}
							cardTerminal.waitForCardAbsent(1000);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
						notifyStatus(TerminalStatus.DISCONNECTED);
					}
				}
			}
			catch (CardException e) {
			}
		}
	}

	private void handleCard(Card card) {
		byte[] historicalBytes = card.getATR().getHistoricalBytes();
		TagType tagType = AcsTagUtils.identifyTagType(historicalBytes);
		tagListener.onTag(new AcsTag(tagType, historicalBytes, card));
	}

	private void notifyStatus(TerminalStatus status) {
		if (statusListener != null)
			statusListener.onStatusChanged(status);
	}

}