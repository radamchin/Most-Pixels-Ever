﻿/*	AS3 Port of Daniel Shiffmans MostPixelsEver - client library		AirTestOne	Test the FileParser / ini reading code	*/package mpe.examples {		import mpe.config.FileParser;		import flash.display.MovieClip;	import flash.utils.getTimer;		public class AirTestOne extends MovieClip{				private var fp:FileParser;		public function AirTestOne(){			trace("AirTestOne:" + getTimer());						var failp:FileParser = new FileParser("app:/dataZ/mpe.ini"); // should fail.						fp = new FileParser("app:/data/mpe.ini");						var id:int = fp.getIntValue("id");			var port:int = fp.getIntValue("Port");						trace("id=" + id);			trace("port=" + port);						var debug:Boolean = fp.getIntValue("debug") == 1;			trace("debug=" + debug);						var server:String = fp.getStringValue("server");			trace("server=" + server)						var localScreenSizeStr:String = fp.getStringValue("localScreenSize");			trace("(String) localScreenSize=" + localScreenSizeStr);						var localScreenSizeInts:Vector.<int> =  fp.getIntValues("localScreenSize");			trace("(Vector.<int>) localScreenSize=" + localScreenSizeInts);						var masterDimensions:Vector.<int> =  fp.getIntValues("masterDimensions");			trace("(Vector.<int>) masterDimensions=" + masterDimensions);						var blahStr:String = fp.getStringValue("blah");			trace("blahStr=" + blahStr);						var blahInt:int = fp.getIntValue("blah");			trace("blahInt=" + blahInt);						var blahInts:Vector.<int> = fp.getIntValues("blah");			trace("blahInts=" + blahInts);					}			}}