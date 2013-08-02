/**
 * Data Listener Interface
 * <http://mostpixelsever.com>
 * @author Shiffman and Kairalla
 */

package mpe.client;

public interface MpeDataListener {
	
	public void mpeFrameEvent(TCPClient c);
	
	public void mpeResetEvent(TCPClient c);
	
}
