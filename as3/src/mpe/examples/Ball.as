/** * Ball class for simple bouncing ball demo * <http://http://code.google.com/p/mostpixelsever/> *///--------------------------------------// A Ball moves and bounces off walls.package mpe.examples {		import mpe.client.TCPClient;	import com.gskinner.utils.Rndm;	import flash.display.Graphics;		public class Ball {		//--------------------------------------		public var x:Number = 0;     // center x position		public var y:Number = 0;     // center y position		public  var xDir:Number = 1;  // x velocity		public var yDir:Number = 1;  // y velocity	 	public var r:Number = 18;    // radius		private var client:TCPClient; // mpe instance			private var col:uint;				public static const MAKE_BALL:String = "B";				//--------------------------------------		public function Ball(mpe:TCPClient, _x:Number, _y:Number) {	    	x = _x;	    	y = _y;						client = mpe;						col = Rndm.integer(0, (255 * 255 * 255));	    	xDir = Rndm.float(-5,5);	    	yDir = Rndm.float(-5,5);						//trace("new Ball: ", x, y, col, "[", xDir, yDir, "]");	  	}		//--------------------------------------		// Moves and changes direction if it hits a wall.		public function calc():void {						if((x-r) < 0) {				x = r;				xDir *= -1;			}else if((x+r) > client.getMWidth()) {				x = client.getMWidth() - r;				xDir *= -1;			}	    				if((y-r) < 0){				y = r;	 			yDir *= -1;			}else if((y+r) > client.getMHeight()) {				y = client.getMHeight() - r;				yDir *= -1;			}				x += xDir;			y += yDir;		}		//--------------------------------------				public function draw(g:Graphics):void {			g.lineStyle(1, 0x000000, 1.0);			g.beginFill(col, 1.0);			g.drawCircle(x-client.getXoffset(), y-client.getYoffset(), r);			g.endFill();		}			}}