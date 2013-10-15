/*
 * Copyright (C) 2008 The Android Open Source Project
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

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Math.h"
#include "b_BasicEm/Functions.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ external functions } ----------------------------------------- */
/*                                                                           */
/* ========================================================================= */

#if defined( HW_SSE2 )
	extern int32 bbs_dotProduct_128SSE2( const int16* vec1A, const int16* vec2A, uint32 sizeA );
	extern int32 bbs_dotProduct_u128SSE2( const int16* vec1A, const int16* vec2A, uint32 sizeA );
#endif

#if defined( HW_FR71 )
	int32 bbs_dotProduct_fr71( const int16* vec1A, const int16* vec2A, uint32 sizeA );
#endif

/* ------------------------------------------------------------------------- */

uint16 bbs_sqrt32( uint32 valA )
{
	uint32 rootL = 0;
	uint32 expL = 0;
	expL += ( ( valA >> ( expL + 0x10 ) ) != 0 ) << 4;
	expL += ( ( valA >> ( expL + 0x08 ) ) != 0 ) << 3;
	expL += ( ( valA >> ( expL + 0x04 ) ) != 0 ) << 2;
	expL += ( ( valA >> ( expL + 0x02 ) ) != 0 ) << 1;
	switch( expL >> 1 )
	{
		case 15: rootL += ( ( rootL + 0x8000 ) * ( rootL + 0x8000 ) <= valA ) << 15;
		case 14: rootL += ( ( rootL + 0x4000 ) * ( rootL + 0x4000 ) <= valA ) << 14;
		case 13: rootL += ( ( rootL + 0x2000 ) * ( rootL + 0x2000 ) <= valA ) << 13;
		case 12: rootL += ( ( rootL + 0x1000 ) * ( rootL + 0x1000 ) <= valA ) << 12;
		case 11: rootL += ( ( rootL + 0x0800 ) * ( rootL + 0x0800 ) <= valA ) << 11;
		case 10: rootL += ( ( rootL + 0x0400 ) * ( rootL + 0x0400 ) <= valA ) << 10;
		case 9:  rootL += ( ( rootL + 0x0200 ) * ( rootL + 0x0200 ) <= valA ) << 9;
		case 8:  rootL += ( ( rootL + 0x0100 ) * ( rootL + 0x0100 ) <= valA ) << 8;
		case 7:  rootL += ( ( rootL + 0x0080 ) * ( rootL + 0x0080 ) <= valA ) << 7;
		case 6:  rootL += ( ( rootL + 0x0040 ) * ( rootL + 0x0040 ) <= valA ) << 6;
		case 5:  rootL += ( ( rootL + 0x0020 ) * ( rootL + 0x0020 ) <= valA ) << 5;
		case 4:  rootL += ( ( rootL + 0x0010 ) * ( rootL + 0x0010 ) <= valA ) << 4;
		case 3:  rootL += ( ( rootL + 0x0008 ) * ( rootL + 0x0008 ) <= valA ) << 3;
		case 2:  rootL += ( ( rootL + 0x0004 ) * ( rootL + 0x0004 ) <= valA ) << 2;
		case 1:  rootL += ( ( rootL + 0x0002 ) * ( rootL + 0x0002 ) <= valA ) << 1;
		case 0:  rootL += ( ( rootL + 0x0001 ) * ( rootL + 0x0001 ) <= valA ) << 0;
	}

	return ( uint16 )rootL;
}

/* ------------------------------------------------------------------------- */

uint8 bbs_sqrt16( uint16 valA )
{
	uint16 rootL = 0;
	rootL += ( ( rootL + 0x80 ) * ( rootL + 0x80 ) <= valA ) << 7;
	rootL += ( ( rootL + 0x40 ) * ( rootL + 0x40 ) <= valA ) << 6;
	rootL += ( ( rootL + 0x20 ) * ( rootL + 0x20 ) <= valA ) << 5;
	rootL += ( ( rootL + 0x10 ) * ( rootL + 0x10 ) <= valA ) << 4;
	rootL += ( ( rootL + 0x08 ) * ( rootL + 0x08 ) <= valA ) << 3;
	rootL += ( ( rootL + 0x04 ) * ( rootL + 0x04 ) <= valA ) << 2;
	rootL += ( ( rootL + 0x02 ) * ( rootL + 0x02 ) <= valA ) << 1;
	rootL += ( ( rootL + 0x01 ) * ( rootL + 0x01 ) <= valA ) << 0;
	return ( uint8 )rootL;
}

/* ------------------------------------------------------------------------- */

/* table of sqrt and slope values */
const uint32 bbs_fastSqrt32_tableG[] = 
{
	268435456, 1016, 272596992, 1000, 276692992, 987, 280735744, 972, 
	284717056, 959, 288645120, 946, 292519936, 933, 296341504, 922, 
	300118016, 910, 303845376, 899, 307527680, 889, 311169024, 878, 
	314765312, 869, 318324736, 858, 321839104, 850, 325320704, 840, 
	328761344, 832, 332169216, 824, 335544320, 815, 338882560, 807, 
	342188032, 799, 345460736, 792, 348704768, 785, 351920128, 777, 
	355102720, 771, 358260736, 764, 361390080, 757, 364490752, 751, 
	367566848, 745, 370618368, 739, 373645312, 732, 376643584, 727, 
	379621376, 722, 382578688, 715, 385507328, 711, 388419584, 705, 
	391307264, 700, 394174464, 695, 397021184, 689, 399843328, 686, 
	402653184, 680, 405438464, 675, 408203264, 672, 410955776, 666, 
	413683712, 663, 416399360, 658, 419094528, 653, 421769216, 650, 
	424431616, 646, 427077632, 641, 429703168, 638, 432316416, 634, 
	434913280, 630, 437493760, 627, 440061952, 622, 442609664, 620, 
	445149184, 615, 447668224, 613, 450179072, 609, 452673536, 605, 
	455151616, 602, 457617408, 600, 460075008, 595, 462512128, 593, 
	464941056, 590, 467357696, 587, 469762048, 583, 472150016, 581, 
	474529792, 578, 476897280, 575, 479252480, 572, 481595392, 569, 
	483926016, 567, 486248448, 564, 488558592, 561, 490856448, 559, 
	493146112, 556, 495423488, 553, 497688576, 552, 499949568, 548, 
	502194176, 546, 504430592, 544, 506658816, 541, 508874752, 539, 
	511082496, 537, 513282048, 534, 515469312, 533, 517652480, 529, 
	519819264, 528, 521981952, 526, 524136448, 523, 526278656, 521, 
	528412672, 519, 530538496, 517, 532656128, 515, 534765568, 514 
};

uint16 bbs_fastSqrt32( uint32 valA )
{
	uint32 expL = 0;
	uint32 valL;
	uint32 offsL;
	uint32 indexL = 0;

	if( valA == 0 ) return 0;

	/* compute closest even size exponent of valA */
	expL += ( ( valA >> ( expL + 0x10 ) ) != 0 ) << 4;
	expL += ( ( valA >> ( expL + 0x08 ) ) != 0 ) << 3;
	expL += ( ( valA >> ( expL + 0x04 ) ) != 0 ) << 2;
	expL += ( ( valA >> ( expL + 0x02 ) ) != 0 ) << 1;

	valL = ( ( valA << ( 30 - expL ) ) - 1073741824 ); /* ( 1 << 30 ) */
	offsL = ( ( valL & 0x01FFFFFF ) + ( 1 << 12 ) ) >> 13;
	indexL = ( valL >> 24 ) & 0xFE;

	return ( bbs_fastSqrt32_tableG[ indexL ] + offsL * bbs_fastSqrt32_tableG[ indexL + 1 ] ) >> ( 28 - ( expL >> 1 ) );
}

/* ------------------------------------------------------------------------- */

/* table of 1/sqrt (1.31) and negative slope (.15) values 
   referenced in b_GaborCueEm/focusDispAsm.s55, do not rename or remove ! */
const uint32 bbs_invSqrt32_tableG[] = 
{
	2147483648u, 1001, 2114682880, 956, 2083356672, 915, 2053373952, 877,
	2024636416, 840, 1997111296, 808, 1970634752, 776, 1945206784, 746,
	1920761856, 720, 1897168896, 693, 1874460672, 669, 1852538880, 646,
	1831370752, 625, 1810890752, 604, 1791098880, 584, 1771962368, 567,
	1753382912, 548, 1735426048, 533, 1717960704, 516, 1701052416, 502,
	1684602880, 487, 1668644864, 474, 1653112832, 461, 1638006784, 448,
	1623326720, 436, 1609039872, 426, 1595080704, 414, 1581514752, 404,
	1568276480, 394, 1555365888, 384, 1542782976, 375, 1530494976, 367,
	1518469120, 357, 1506770944, 350, 1495302144, 342, 1484095488, 334,
	1473150976, 327, 1462435840, 320, 1451950080, 313, 1441693696, 307,
	1431633920, 300, 1421803520, 294, 1412169728, 289, 1402699776, 282,
	1393459200, 277, 1384382464, 272, 1375469568, 266, 1366753280, 262,
	1358168064, 257, 1349746688, 251, 1341521920, 248, 1333395456, 243,
	1325432832, 238, 1317634048, 235, 1309933568, 230, 1302396928, 227,
	1294958592, 222, 1287684096, 219, 1280507904, 216, 1273430016, 211,
	1266515968, 209, 1259667456, 205, 1252950016, 202, 1246330880, 198,
	1239842816, 196, 1233420288, 192, 1227128832, 190, 1220902912, 187,
	1214775296, 184, 1208745984, 181, 1202814976, 179, 1196949504, 176,
	1191182336, 173, 1185513472, 171, 1179910144, 169, 1174372352, 166,
	1168932864, 164, 1163558912, 162, 1158250496, 160, 1153007616, 157,
	1147863040, 155, 1142784000, 154, 1137737728, 151, 1132789760, 149,
	1127907328, 148, 1123057664, 145, 1118306304, 144, 1113587712, 142,
	1108934656, 140, 1104347136, 138, 1099825152, 137, 1095335936, 135,
	1090912256, 134, 1086521344, 131, 1082228736, 131, 1077936128, 128		
};

uint32 bbs_invSqrt32( uint32 valA )
{

	uint32 expL = 0;
	uint32 valL;
	uint32 offsL;
	uint32 indexL = 0;

	if( valA == 0U ) return 0x80000000;

	/* compute closest even size exponent of valA */
	expL += ( ( valA >> ( expL + 0x10 ) ) != 0 ) << 4;
	expL += ( ( valA >> ( expL + 0x08 ) ) != 0 ) << 3;
	expL += ( ( valA >> ( expL + 0x04 ) ) != 0 ) << 2;
	expL += ( ( valA >> ( expL + 0x02 ) ) != 0 ) << 1;
	
	valL = ( ( valA << ( 30 - expL ) ) - 1073741824 ); /* ( 1 << 30 ) */
	offsL = ( ( valL & 0x01FFFFFF ) + ( 1 << 9 ) ) >> 10;
	indexL = ( valL >> 24 ) & 0xFE;
	
	return ( bbs_invSqrt32_tableG[ indexL ] - offsL * bbs_invSqrt32_tableG[ indexL + 1 ] ) >> ( expL >> 1 );
}

/* ------------------------------------------------------------------------- */

/* table of 1/( x + 1 ) (2.30) and negative slope (.14) values
   referenced in b_GaborCueEm/focusDispAsm.s55, do not rename or remove ! */
const int32 bbs_inv32_tableG[] = 
{
	1073741824, 1986, 1041203200, 1870, 1010565120, 1762, 981696512, 1664,
	954433536,  1575, 928628736,  1491, 904200192,  1415, 881016832, 1345,
	858980352,  1278, 838041600,  1218, 818085888,  1162, 799047680, 1108,
	780894208,  1059, 763543552,  1013, 746946560,  970,  731054080, 930,
	715816960,  891,  701218816,  856,  687194112,  823,  673710080, 791,
	660750336,  761,  648282112,  732,  636289024,  706,  624721920, 681,
	613564416,  657,  602800128,  635,  592396288,  613,  582352896, 592,
	572653568,  573,  563265536,  554,  554188800,  537,  545390592, 520,
};

int32 bbs_inv32( int32 valA )
{

	uint32 expL = 0;
	int32 signL = ( ( valA >> 30 ) & 0xFFFFFFFE ) + 1;
	int32 valL = signL * valA;
	int32 offsL;
	uint32 indexL = 0;

	if( valL <= ( int32 ) 1 ) return 0x40000000 * signL;

	/* compute size exponent of valL */
	expL += ( ( valL >> ( expL + 0x10 ) ) != 0 ) << 4;
	expL += ( ( valL >> ( expL + 0x08 ) ) != 0 ) << 3;
	expL += ( ( valL >> ( expL + 0x04 ) ) != 0 ) << 2;
	expL += ( ( valL >> ( expL + 0x02 ) ) != 0 ) << 1;
	expL += ( ( valL >> ( expL + 0x01 ) ) != 0 );
	
	valL = ( ( valL << ( 30 - expL ) ) - 1073741824 ); /*( 1U << 30 )*/
	offsL = ( ( valL & 0x01FFFFFF ) + ( 1 << 10 ) ) >> 11;
	indexL = ( valL >> 24 ) & 0xFE;
	
	return signL * ( ( ( ( bbs_inv32_tableG[ indexL ] - offsL * bbs_inv32_tableG[ indexL + 1 ] ) >> ( expL - 1 ) ) + 1 ) >> 1 );
}

/* ------------------------------------------------------------------------- */

uint32 bbs_intLog2( uint32 valA )
{
	uint32 expL = 0;
	expL += 0x10 * ( ( valA >> ( expL + 0x10 ) ) != 0 );
	expL += 0x08 * ( ( valA >> ( expL + 0x08 ) ) != 0 );
	expL += 0x04 * ( ( valA >> ( expL + 0x04 ) ) != 0 );
	expL += 0x02 * ( ( valA >> ( expL + 0x02 ) ) != 0 );
	expL += 0x01 * ( ( valA >> ( expL + 0x01 ) ) != 0 );
	return expL;
}

/* ------------------------------------------------------------------------- */

const uint32 bbs_pow2M1_tableG[] = 
{
	0,			713,	46769127,	721,	94047537,	729,	141840775,	737,
	190154447,	745,	238994221,	753,	288365825,	761,	338275050,	769,
	388727751,	778,	439729846,	786,	491287318,	795,	543406214,	803,
	596092647,	812,	649352798,	821,	703192913,	830,	757619310,	839,
	812638371,	848,	868256550,	857,	924480371,	867,	981316430,	876,
	1038771393, 886,	1096851999, 895,	1155565062, 905,	1214917468, 915,
	1274916179, 925,	1335568233, 935,	1396880745, 945,	1458860907, 956,
	1521515988, 966,	1584853338, 976,	1648880387, 987,	1713604645, 998,
	1779033703, 1009,	1845175238, 1020,	1912037006, 1031,	1979626852, 1042,
	2047952702, 1053,	2117022572, 1065,	2186844564u, 1077,	2257426868u, 1088,
	2328777762u, 1100,	2400905617u, 1112,	2473818892u, 1124,	2547526141u, 1136,
	2622036010u, 1149,	2697357237u, 1161,	2773498659u, 1174,	2850469207u, 1187,
	2928277909u, 1200,	3006933892u, 1213,	3086446383u, 1226,	3166824708u, 1239,
	3248078296u, 1253,	3330216677u, 1266,	3413249486u, 1280,	3497186464u, 1294,
	3582037455u, 1308,	3667812413u, 1323,	3754521399u, 1337,	3842174584u, 1352,
	3930782250u, 1366,	4020354790u, 1381,	4110902710u, 1396,	4202436633u, 1411
};

uint32 bbs_pow2M1( uint32 valA )
{
	uint32 offsL = ( valA & 0x03FFFFFF ) >> 10;
	uint16 indexL = ( ( valA & 0xFC000000 ) >> 26 ) << 1;
	return bbs_pow2M1_tableG[ indexL ] + offsL * bbs_pow2M1_tableG[ indexL + 1 ];	
}

/* ------------------------------------------------------------------------- */

uint32 bbs_pow2( int32 valA )
{
	int32 shiftL = 16 - ( valA >> 27 );
	uint32 offsL  = ( uint32 )( valA << 5 );
	if( shiftL == 32 ) return 1;
	return ( 1 << ( 32 - shiftL ) ) + ( bbs_pow2M1( offsL ) >> shiftL );
}

/* ------------------------------------------------------------------------- */

uint32 bbs_exp( int32 valA )
{
	int32 adjustedL;
	int32 shiftL;
	int32 offsL;

	/* check boundaries to avoid overflow */
	if( valA < -1488522236 )
	{
		return 0;
	}
	else if( valA > 1488522236 )
	{
		return 0xFFFFFFFF;
	}

	/* multily valA with 1/ln(2) in order to use function 2^x instead of e^x */
	adjustedL = ( valA >> 16 ) * 94548 + ( ( ( ( ( uint32 )valA ) & 0x0FFFF ) * 47274 ) >> 15 );

	shiftL = 16 - ( adjustedL >> 27 );
	if( shiftL == 32 ) return 1;
	offsL  = ( uint32 )( adjustedL << 5 );
	return ( ( int32 ) 1 << ( 32 - shiftL ) ) + ( bbs_pow2M1( offsL ) >> shiftL );
}

/* ------------------------------------------------------------------------- */

int16 bbs_satS16( int32 valA )
{
	if( valA > 32767 ) return 32767;
	if( valA < -32768 ) return -32768;
	return valA;
}

/* ------------------------------------------------------------------------- */

#if defined( HW_i586 ) || defined( HW_i686 )

/* Windows section */
#if defined( WIN32 ) && !defined( WIN64 )

/* disable warning "no return value"*/
#pragma warning( disable : 4035 )

/** 
 * computes a fast dot product using intel MMX, sizeA must be multiple of 16 and >0 
 */
int32 bbs_dotProduct_intelMMX16( const int16* vec1A, const int16* vec2A, uint32 sizeA )
{
	__asm
	{	
			push    esi
			push    edi

			mov     eax, vec1A
			mov     ebx, vec2A

			mov     ecx, sizeA

			pxor    mm4, mm4
			pxor    mm6, mm6

			pxor    mm7, mm7
			shr		ecx, 4

		inner_loop_start:
			movq    mm0, 0[eax]
			paddd   mm7, mm4

			movq    mm1, 0[ebx]
			paddd   mm7, mm6

			movq    mm2, 8[eax]
			pmaddwd mm0, mm1

			movq    mm3, 8[ebx]

			movq    mm4, 16[eax]
			pmaddwd mm2, mm3

			movq    mm5, 16[ebx]
			paddd   mm7, mm0

			movq    mm6, 24[eax]
			pmaddwd mm4, mm5

			pmaddwd mm6, 24[ebx]
			paddd   mm7, mm2

			add     eax, 32
			add     ebx, 32

			dec     ecx
			jnz     inner_loop_start

			paddd   mm7, mm4

			paddd   mm7, mm6
        
			movq    mm0, mm7

			psrlq   mm0, 32

			paddd   mm7, mm0

			movd    eax, mm7
			
			emms
			pop     edi
			pop     esi
	}
}

#pragma warning( default : 4035 )

/* gcc compiler section */
#elif defined( epl_LINUX ) || defined( CYGWIN )

/**
 * computes a fast dot product using intel MMX, sizeA must be multiple of 16 and >0
 */
int32 bbs_dotProduct_intelMMX16( const int16* vec1A, const int16* vec2A, uint32 sizeA )
{
	int32 resultL;

	__asm__ __volatile__(

			"movl %1,%%eax\n\t"
			"movl %2,%%ebx\n\t"

			"movl %3,%%ecx\n\t"

			"pxor %%mm4,%%mm4\n\t"
			"pxor %%mm6,%%mm6\n\t"

			"pxor %%mm7, %%mm7\n\t"
			"shrl $4, %%ecx\n\t"

			"\n1:\t"
			"movq 0(%%eax),%%mm0\n\t"
			"paddd %%mm4,%%mm7\n\t"

			"movq 0( %%ebx ),%%mm1\n\t"
			"paddd %%mm6,%%mm7\n\t"
			
			"movq 8( %%eax ),%%mm2\n\t"
			"pmaddwd %%mm1,%%mm0\n\t"

			"movq 8( %%ebx ),%%mm3\n\t"

			"movq 16( %%eax ),%%mm4\n\t"
			"pmaddwd %%mm3,%%mm2\n\t"

			"movq 16( %%ebx ),%%mm5\n\t"
			"paddd %%mm0,%%mm7\n\t"

			"movq 24( %%eax ),%%mm6\n\t"
			"pmaddwd %%mm5,%%mm4\n\t"

			"pmaddwd 24( %%ebx ),%%mm6\n\t"
			"paddd %%mm2,%%mm7\n\t"

			"addl $32,%%eax\n\t"
			"addl $32,%%ebx\n\t"

			"decl %%ecx\n\t"
			"jnz 1b\n\t"

			"paddd %%mm4,%%mm7\n\t"
			"paddd %%mm6,%%mm7\n\t"
        
			"movq  %%mm7,%%mm0\n\t"

			"psrlq $32,%%mm0\n\t"

			"paddd %%mm0,%%mm7\n\t"

			"movd %%mm7,%0\n\t"
			
			"emms\n\t"

		: "=&g" ( resultL )
		: "g" ( vec1A ), "g" ( vec2A ), "g" ( sizeA )
		: "si", "di", "ax", "bx", "cx", "st", "memory" );

	return resultL;
}

#endif /* epl_LINUX, CYGWIN */

#endif /* HW_i586 || HW_i686 */

/* ------------------------------------------------------------------------- */

#ifdef HW_TMS320C6x
/**
 * Calls fast assembler version of dotproduct for DSP. 
 * dotProduct_C62x is implemented in file dotprod.asm and expects input vectors
 * of even length.
 */
int32 bbs_dotProduct_dsp( const int16* vec1A, const int16* vec2A, uint32 sizeA )
{
	if( sizeA & 1 )
	{
		int32 resultL;		
		resultL = dotProduct_C62x( vec1A, vec2A, sizeA - 1 );
		return resultL + ( int32 ) *( vec1A + sizeA - 1 ) * *( vec2A + sizeA - 1 );
	}
	else
	{
		return dotProduct_C62x( vec1A, vec2A, sizeA );
	}
}
#endif /* HW_TMS320C6x */

/* ------------------------------------------------------------------------- */

/* 16 dot product for the PS2/EE processor */
/* input vectors MUST be 128 bit aligned ! */

#if defined( epl_LINUX ) && defined( HW_EE )

int32 bbs_dotProduct_EE( const int16* vec1A, const int16* vec2A, uint32 sizeA )
{
	int32 resultL = 0,
	      iL = sizeA >> 3,
	      jL = sizeA - ( iL << 3 );

	if( iL > 0 )
	{
		/* multiply-add elements of input vectors in sets of 8 */
		int32 accL[ 4 ], t1L, t2L, t3L;
		asm volatile (
			"pxor %4, %2, %2\n\t"			/* reset 8 accumulators (LO and HI register) to 0 */
			"pmtlo %4\n\t"		
			"pmthi %4\n\t"

			"\n__begin_loop:\t"

			"lq %2,0(%0)\n\t"				/* load 8 pairs of int16 */
			"lq %3,0(%1)\n\t"			

			"addi %0,%0,16\n\t"				/* vec1L += 16 */
			"addi %1,%1,16\n\t"				/* vec2L += 16 */
			"addi %7,%7,-1\n\t"				/* iL-- */

			"pmaddh %4,%2,%3\n\t"			/* parallel multiply-add of 8 pairs of int16 */

			"bgtzl %7,__begin_loop\n\t"		/* if iL > 0 goto _begin_loop */

			"pmflo %2\n\t"					/* parallel add 8 accumulators , store remaining 4 accumulators in tmpL */
			"pmfhi %3\n\t"
			"paddw %4,%2,%3\n\t"						
			"sq %4,0(%8)\n\t"
		: "=r" ( vec1A ), "=r" ( vec2A ), "=r" ( t1L ), "=r" ( t2L ), "=r" ( t3L )
		: "0" ( vec1A ), "1" ( vec2A ), "r" ( iL ), "r" ( accL )
		: "memory" );

		/* add 4 parallel accumulators */
		resultL += accL[ 0 ] + accL[ 1 ] + accL[ 2 ] + accL[ 3 ];
	}
	
	/* multiply-add remaining elements of input vectors */
	for( ; jL--; ) resultL += ( int32 ) *vec1A++ * *vec2A++;

	return resultL;
}

#endif

/* ------------------------------------------------------------------------- */

#if defined( HW_ARMv5TE )

/* fast 16 dot product for ARM9E cores (DSP extensions).
 * input vectors must be 32 bit aligned
 */
int32 bbs_dotProduct_arm9e( const int16* vec1A, const int16* vec2A, uint32 sizeA )
{
	int32 accuL = 0;

	int32* v1PtrL = ( int32* )vec1A;
	int32* v2PtrL = ( int32* )vec2A;

	for( ; sizeA >= 4; sizeA -= 4 )
	{
		__asm {
		    smlabb accuL, *v1PtrL, *v2PtrL, accuL;
		    smlatt accuL, *v1PtrL, *v2PtrL, accuL;
		}
		v1PtrL++; v2PtrL++;
	    __asm {
		    smlabb accuL, *v1PtrL, *v2PtrL, accuL;
		    smlatt accuL, *v1PtrL, *v2PtrL, accuL;
		}
		v1PtrL++; v2PtrL++;
	}

	vec1A = ( int16* )v1PtrL;
	vec2A = ( int16* )v2PtrL;

	/* multiply-add remaining elements of input vectors */
	for( ; sizeA > 0; sizeA-- ) accuL += ( int32 )*vec1A++ * *vec2A++;

	return accuL;
}

#endif

/* ------------------------------------------------------------------------- */

/**
 * Computes a fast dot product using standard C
 */
int32 bbs_dotProduct_stdc( const int16* vec1A, const int16* vec2A, uint32 sizeA )
{
	int32 accuL = 0;

	for( ; sizeA >= 8; sizeA -= 8 )
	{
		accuL += ( int32 ) *vec1A * *vec2A;
		accuL += ( int32 ) *( vec1A + 1 ) * *( vec2A + 1 );
		accuL += ( int32 ) *( vec1A + 2 ) * *( vec2A + 2 );
		accuL += ( int32 ) *( vec1A + 3 ) * *( vec2A + 3 );

		accuL += ( int32 ) *( vec1A + 4 ) * *( vec2A + 4 );
		accuL += ( int32 ) *( vec1A + 5 ) * *( vec2A + 5 );
		accuL += ( int32 ) *( vec1A + 6 ) * *( vec2A + 6 );
		accuL += ( int32 ) *( vec1A + 7 ) * *( vec2A + 7 );

		vec1A += 8;
		vec2A += 8;
	}

	for( ; sizeA; sizeA-- ) accuL += ( int32 ) *vec1A++ * *vec2A++;

	return accuL;
}

/* ------------------------------------------------------------------------- */

int32 bbs_dotProductInt16( const int16* vec1A, const int16* vec2A, uint32 sizeA )
{
/* PC */
#if ( defined( HW_i586 ) || defined( HW_i686 ) )

	#if defined( HW_SSE2 )
		uint32 size16L = sizeA & 0xfffffff0;
		if( size16L )
		{	
			if( ( (uint32)vec1A & 0xF ) == 0 && ( (uint32)vec2A & 0xF ) == 0 ) 
			{
				return bbs_dotProduct_128SSE2( vec1A, vec2A, sizeA );
			}
			else
			{
				return bbs_dotProduct_u128SSE2( vec1A, vec2A, sizeA );
			}
		}
	#elif !defined( WIN64 )
		/* MMX version (not supported by 64-bit compiler) */
		uint32 size16L = sizeA & 0xfffffff0;
		if( size16L )
		{	
			if( sizeA == size16L )
			{
				return bbs_dotProduct_intelMMX16( vec1A, vec2A, size16L );
			}
			return bbs_dotProduct_intelMMX16( vec1A, vec2A, size16L )
					+ bbs_dotProduct_stdc( vec1A + size16L, vec2A + size16L, sizeA - size16L );
		} /* if( size16L ) */
	#endif

	return bbs_dotProduct_stdc( vec1A, vec2A, sizeA );

/* Playstation 2 */
#elif defined( HW_EE ) && defined( epl_LINUX ) 

	if( ( (uint32)vec1A & 0xF ) == 0 && ( (uint32)vec2A & 0xF ) == 0 ) 
	{
		return bbs_dotProduct_EE( vec1A, vec2A, sizeA );
	}
	return bbs_dotProduct_stdc( vec1A, vec2A, sizeA );

/* ARM9E */
#elif defined( HW_ARMv5TE )

	return bbs_dotProduct_arm9e( vec1A, vec2A, sizeA );

/* TI C6000 */
#elif defined( HW_TMS320C6x )

	return bbs_dotProduct_dsp( vec1A, vec2A, sizeA );
	
#elif defined( HW_FR71 )

	uint32 size16L = sizeA & 0xfffffff0;
	if( size16L )
	{	
		if( sizeA == size16L )
		{
			return bbs_dotProduct_fr71( vec1A, vec2A, size16L );
		}
		return bbs_dotProduct_fr71( vec1A, vec2A, size16L )
				+ bbs_dotProduct_stdc( vec1A + size16L, vec2A + size16L, sizeA - size16L );
	}

	return bbs_dotProduct_stdc( vec1A, vec2A, sizeA );

#endif

	return bbs_dotProduct_stdc( vec1A, vec2A, sizeA );
}

/* ------------------------------------------------------------------------- */

/* table of fermi and slope values (result: 2.30; offset: .12) 
   referenced in b_NeuralNetEm/FastMlpNet.c, not not rename or remove */
const uint32 bbs_fermi_tableG[] = 
{
	45056,      8,     77824,      13,    131072,     21,    217088,     34,
	356352,     57,    589824,     94,    974848,     155,   1609728,    255,
	2654208,    418,   4366336,    688,   7184384,    1126,  11796480,   1834,
	19308544,   2970,  31473664,   4748,  50921472,   7453,  81448960,   11363,
	127991808,  16573, 195874816,  22680, 288772096,  28469, 405381120,  32102,
	536870912,  32101, 668356608,  28469, 784965632,  22680, 877862912,  16573,
	945745920,  11363, 992288768,  7453,  1022816256, 4748,  1042264064, 2970,
	1054429184, 1834,  1061941248, 1126,  1066553344, 688,   1069371392, 418,
	1071083520, 255,   1072128000, 155,   1072762880, 94,    1073147904, 57,
	1073381376, 34,    1073520640, 21,    1073606656, 13,    1073659904, 8,
};

int32 bbs_fermi( int32 valA )
{
	uint32 indexL = ( ( valA >> 15 ) + 20 ) << 1;
	uint32 offsL  = ( ( valA & 0x00007FFF ) + 4 ) >> 3;
	if( valA <  -655360 ) return 1;
	if( valA >=  655360 ) return 1073741824 - 1; /* ( 1 << 30 ) */
	return ( bbs_fermi_tableG[ indexL ] + offsL * bbs_fermi_tableG[ indexL + 1 ] );
}

/* ------------------------------------------------------------------------- */

void bbs_uint32ReduceToNBits( uint32* argPtrA, int32* bbpPtrA, uint32 nBitsA )
{
	int32 posHighestBitL = bbs_intLog2( *argPtrA ) + 1;
	int32 shiftL = posHighestBitL - nBitsA;
	if( shiftL > 0 )
	{
		( *argPtrA ) >>= shiftL;
		( *bbpPtrA ) -= shiftL;
	}
}

/* ------------------------------------------------------------------------- */

void bbs_int32ReduceToNBits( int32* argPtrA, int32* bbpPtrA, uint32 nBitsA )
{
	int32 posHighestBitL = bbs_intLog2( bbs_abs( *argPtrA ) ) + 1;
	int32 shiftL = posHighestBitL - nBitsA;
	if( shiftL > 0 )
	{
		( *argPtrA ) >>= shiftL;
		( *bbpPtrA ) -= shiftL;
	}
}

/* ------------------------------------------------------------------------- */

uint32 bbs_convertU32( uint32 srcA, int32 srcBbpA, int32 dstBbpA )
{
	if( dstBbpA >= srcBbpA )
	{
		uint32 shiftL = dstBbpA - srcBbpA;
		if( srcA > ( ( uint32 )0xFFFFFFFF >> shiftL ) )
		{
			/* overflow */
			return ( uint32 )0xFFFFFFFF;
		}
		else
		{
			return srcA << shiftL;
		}
	}
	else
	{
		uint32 shiftL = srcBbpA - dstBbpA;
		uint32 addL = 1L << ( shiftL - 1 );
		if( srcA + addL < addL )
		{
			/* rounding would cause overflow */
			return srcA >> shiftL;
		}
		else
		{
			return ( srcA + addL ) >> shiftL;
		}
	}
}

/* ------------------------------------------------------------------------- */

int32 bbs_convertS32( int32 srcA, int32 srcBbpA, int32 dstBbpA )
{
	if( dstBbpA >= srcBbpA )
	{
		uint32 shiftL = ( uint32 )( dstBbpA - srcBbpA );
		if( srcA > ( ( int32 )0x7FFFFFFF >> shiftL ) )
		{
			/* overflow */
			return ( uint32 )0x7FFFFFFF;
		}
		else if( srcA < ( ( int32 )0x80000000 >> shiftL ) )
		{
			/* underflow */
			return ( int32 )0x80000000;
		}
		else
		{
			return srcA << shiftL;
		}
	}
	else
	{
		uint32 shiftL = ( uint32 )( srcBbpA - dstBbpA );
		int32 addL = 1L << ( shiftL - 1 );
		if( srcA + addL < addL )
		{
			/* rounding would cause overflow */
			return srcA >> shiftL;
		}
		else
		{
			return ( srcA + addL ) >> shiftL;
		}
	}
}

/* ------------------------------------------------------------------------- */

int32 bbs_vecPowerFlt16( const int16 *xA, int16 nxA )
{
/*	#if defined( HW_TMS320C5x )
		uint32 rL;
		power( ( int16* ) xA, ( int32* ) &rL, nxA );  // does not work properly in DSPLib version 2.20.02
		return ( rL >> 1 );
	#else*/
		/* needs to be optimized */
		int32 rL = 0;
		for( ; nxA--; )
		{
			rL += ( int32 ) *xA * *xA;
			xA++;
		}
		return rL;
/*	#endif */
}

/* ------------------------------------------------------------------------- */

void bbs_mulU32( uint32 v1A, uint32 v2A, uint32* manPtrA, int32* expPtrA )
{
	uint32 log1L = bbs_intLog2( v1A );
	uint32 log2L = bbs_intLog2( v2A );

	if( log1L + log2L < 32 )
	{
		*manPtrA = v1A * v2A;
		*expPtrA = 0;
	}
	else
	{
		uint32 v1L = v1A;
		uint32 v2L = v2A;
		uint32 exp1L = 0;
		uint32 exp2L = 0;
		if( log1L > 15 && log2L > 15 ) 
		{
			exp1L = log1L - 15;
			exp2L = log2L - 15;
			v1L = ( ( v1L >> ( exp1L - 1 ) ) + 1 ) >> 1;
			v2L = ( ( v2L >> ( exp2L - 1 ) ) + 1 ) >> 1;
		}
		else if( log1L > 15 ) 
		{
			exp1L = log1L + log2L - 31;
			v1L = ( ( v1L >> ( exp1L - 1 ) ) + 1 ) >> 1;
		}
		else
		{
			exp2L = log1L + log2L - 31;
			v2L = ( ( v2L >> ( exp2L - 1 ) ) + 1 ) >> 1;
		}

		*manPtrA = v1L * v2L;
		*expPtrA = exp1L + exp2L;
	}
}

/* ------------------------------------------------------------------------- */

void bbs_mulS32( int32 v1A, int32 v2A, int32* manPtrA, int32* expPtrA )
{
	uint32 log1L = bbs_intLog2( v1A > 0 ? v1A : -v1A );
	uint32 log2L = bbs_intLog2( v2A > 0 ? v2A : -v2A );

	if( log1L + log2L < 30 )
	{
		*manPtrA = v1A * v2A;
		*expPtrA = 0;
	}
	else
	{
		int32 v1L = v1A;
		int32 v2L = v2A;
		int32 exp1L = 0;
		int32 exp2L = 0;
		if( log1L > 14 && log2L > 14 ) 
		{
			exp1L = log1L - 14;
			exp2L = log2L - 14;
			v1L = ( ( v1L >> ( exp1L - 1 ) ) + 1 ) >> 1;
			v2L = ( ( v2L >> ( exp2L - 1 ) ) + 1 ) >> 1;
		}
		else if( log1L > 14 ) 
		{
			exp1L = log1L + log2L - 29;
			v1L = ( ( v1L >> ( exp1L - 1 ) ) + 1 ) >> 1;
		}
		else
		{
			exp2L = log1L + log2L - 29;
			v2L = ( ( v2L >> ( exp2L - 1 ) ) + 1 ) >> 1;
		}

		*manPtrA = v1L * v2L;
		*expPtrA = exp1L + exp2L;
	}
}

/* ------------------------------------------------------------------------- */

void bbs_vecSqrNorm32( const int32* vecA, uint32 sizeA, uint32* manPtrA, uint32* expPtrA )
{
	uint32 sumL = 0;
	int32 sumExpL = 0;

	uint32 iL;
	for( iL = 0; iL < sizeA; iL++ )
	{
		int32 vL = vecA[ iL ];
		int32 logL = bbs_intLog2( vL > 0 ? vL : -vL );
		int32 expL = ( logL > 14 ) ? logL - 14 : 0;
		uint32 prdL;

		if( expL >= 1 )
		{
			vL = ( ( vL >> ( expL - 1 ) ) + 1 ) >> 1;
		}
		else
		{
			vL = vL >> expL;
		}

		prdL = vL * vL;
		expL <<= 1; /* now exponent of product */

		if( sumExpL > expL )
		{
			uint32 shrL = sumExpL - expL;
			prdL = ( ( prdL >> ( shrL - 1 ) ) + 1 ) >> 1;
		}
		else if( expL > sumExpL )
		{
			uint32 shrL = expL - sumExpL;
			sumL = ( ( sumL >> ( shrL - 1 ) ) + 1 ) >> 1;
			sumExpL += shrL;
		}

		sumL += prdL;

		if( sumL > 0x80000000 )
		{
			sumL = ( sumL + 1 ) >> 1;
			sumExpL++;
		}
	}

	/* make exponent even */
	if( ( sumExpL & 1 ) != 0 )
	{
		sumL = ( sumL + 1 ) >> 1;
		sumExpL++;
	}

	if( manPtrA != NULL ) *manPtrA = sumL;
	if( expPtrA != NULL ) *expPtrA = sumExpL;
}

/* ------------------------------------------------------------------------- */

void bbs_vecSqrNorm16( const int16* vecA, uint32 sizeA, uint32* manPtrA, uint32* expPtrA )
{
	uint32 sumL = 0;
	int32 sumExpL = 0;

	uint32 iL;
	for( iL = 0; iL < sizeA; iL++ )
	{
		int32 vL = vecA[ iL ];
		uint32 prdL = vL * vL;

		if( sumExpL > 0 )
		{
			uint32 shrL = sumExpL;
			prdL = ( ( prdL >> ( shrL - 1 ) ) + 1 ) >> 1;
		}

		sumL += prdL;

		if( sumL > 0x80000000 )
		{
			sumL = ( sumL + 1 ) >> 1;
			sumExpL++;
		}
	}

	/* make exponent even */
	if( ( sumExpL & 1 ) != 0 )
	{
		sumL = ( sumL + 1 ) >> 1;
		sumExpL++;
	}

	if( manPtrA != NULL ) *manPtrA = sumL;
	if( expPtrA != NULL ) *expPtrA = sumExpL;
}

/* ------------------------------------------------------------------------- */

uint32 bbs_vecNorm16( const int16* vecA, uint32 sizeA )
{
	uint32 manL;
	uint32 expL;
	bbs_vecSqrNorm16( vecA, sizeA, &manL, &expL );
	manL = bbs_sqrt32( manL );
	return manL << ( expL >> 1 );
}

/* ------------------------------------------------------------------------- */

void bbs_matMultiplyFlt16( const int16 *x1A, int16 row1A, int16 col1A, const int16 *x2A, int16 col2A, int16 *rA )
{
	#if defined( HW_TMS320C5x )
		/* operands need to be in internal memory for mmul*/
		if( x1A > ( int16* ) bbs_C5X_INTERNAL_MEMORY_SIZE ||
			x2A > ( int16* ) bbs_C5X_INTERNAL_MEMORY_SIZE )
		{
			int16 iL,jL,kL;
			int16 *ptr1L, *ptr2L;
			int32 sumL;
			
			for( iL = 0; iL < row1A; iL++ )
			{
				for( jL = 0; jL < col2A; jL++ )
				{
					ptr1L = ( int16* ) x1A + iL * col1A;
					ptr2L = ( int16* ) x2A + jL;
					sumL = 0;
					for( kL = 0; kL < col1A; kL++ )
					{
						sumL += ( ( int32 ) *ptr1L++ * *ptr2L );
						ptr2L += col2A;
					}
					*rA++ = ( sumL + ( 1 << 14 ) ) >> 15; /* round result to 1.15 */
				}
			}
		}
		else mmul( ( int16* ) x1A, row1A, col1A, ( int16* ) x2A, col1A, col2A, rA );
	
	#elif defined( HW_ARMv4 ) || defined( HW_ARMv5TE )

		int32 iL, jL, kL;
		int16 *ptr1L, *ptr2L;
		int32 sumL;
		for( iL = 0; iL < row1A; iL++ )
		{
			for( jL = 0; jL < col2A; jL++ )
			{
				ptr1L = ( int16* ) x1A + iL * col1A;
				ptr2L = ( int16* ) x2A + jL;
				sumL = 0;
				for( kL = col1A; kL >= 4; kL -= 4 )
				{
					sumL += ( ( int32 ) *ptr1L++ * *ptr2L );
					sumL += ( ( int32 ) *ptr1L++ * *( ptr2L += col2A ) );
					sumL += ( ( int32 ) *ptr1L++ * *( ptr2L += col2A ) );
					sumL += ( ( int32 ) *ptr1L++ * *( ptr2L += col2A ) );
					ptr2L += col2A;
				}
				for( ; kL > 0; kL-- )
				{
					sumL += ( ( int32 ) *ptr1L++ * *ptr2L );
					ptr2L += col2A;
				}
				*rA++ = ( sumL + ( 1 << 14 ) ) >> 15; /* round result to 1.15 */
			}
		}
	#else
		/* needs to be optimized */
		int16 iL,jL,kL;
		int16 *ptr1L, *ptr2L;
		int32 sumL;
		
		for( iL = 0; iL < row1A; iL++ )
		{
			for( jL = 0; jL < col2A; jL++ )
			{
				ptr1L = ( int16* ) x1A + iL * col1A;
				ptr2L = ( int16* ) x2A + jL;
				sumL = 0;
				for( kL = 0; kL < col1A; kL++ )
				{
					sumL += ( ( int32 ) *ptr1L++ * *ptr2L );
					ptr2L += col2A;
				}
				*rA++ = ( sumL + ( 1 << 14 ) ) >> 15; /* round result to 1.15 */
			}
		}
	#endif
}

/* ------------------------------------------------------------------------- */

void bbs_matMultiplyTranspFlt16( const int16 *x1A, int16 row1A, int16 col1A, 
								 const int16 *x2A, int16 col2A, int16 *rA )
{
	const int16* ptr1L = x1A;

	int32 iL;
	for( iL = row1A; iL > 0 ; iL-- )
	{
		int32 jL;
		const int16* ptr2L = x2A;
		for( jL = col2A; jL > 0 ; jL-- )
		{
			int32 kL;
			int32 sumL = 0;
			for( kL = col1A >> 2; kL > 0; kL-- )
			{
				sumL += ( ( int32 ) *ptr1L++ * *ptr2L++ );
				sumL += ( ( int32 ) *ptr1L++ * *ptr2L++ );
				sumL += ( ( int32 ) *ptr1L++ * *ptr2L++ );
				sumL += ( ( int32 ) *ptr1L++ * *ptr2L++ );
			}
			for( kL = col1A & 3; kL > 0; kL-- )
			{
				sumL += ( ( int32 ) *ptr1L++ * *ptr2L++ );
			}
			*rA++ = ( sumL + ( 1 << 14 ) ) >> 15; /* round result to 1.15 */
			ptr1L -= col1A;
		}
		ptr1L += col1A;
	}
}

/* ------------------------------------------------------------------------- */


#ifndef mtrans
uint16 bbs_matTrans( int16 *xA, int16 rowA, int16 colA, int16 *rA )
{
	/* needs to be optimized */
	int16 iL;
	for( iL = colA; iL--; )
	{
		int16* sL = xA++;
		int16 jL;
		for( jL = rowA; jL--; )
		{
			*rA++ = *sL;
			sL += colA;
		}
	}
	return 0;
}
#endif

/* ------------------------------------------------------------------------- */
#ifndef atan2_16
int16 bbs_atan2( int16 nomA, int16 denomA )
{
	int16 phL, argL;	
	
	if( nomA == denomA ) return 8192;
	argL = ( ( int32 ) nomA << 15 ) / denomA;
	
	/* 0.318253*2 x      20857 .15
	  +0.003314*2 x^2      217 .15
	  -0.130908*2 x^3    -8580 .15
	  +0.068542*2 x^4     4491 .15
	  -0.009159*2 x^5     -600 .15 */	

	phL = -600;
	phL = ( ( ( int32 ) phL * argL ) >> 15 ) + 4481;
	phL = ( ( ( int32 ) phL * argL ) >> 15 ) - 8580;
	phL = ( ( ( int32 ) phL * argL ) >> 15 ) + 217;
	phL = ( ( ( int32 ) phL * argL ) >> 15 ) + 20857;
	phL = ( ( int32 ) phL * argL ) >> 15;

	return phL >> 1; /* /2 */
}

/* needs to be optimized */
uint16 bbs_vecPhase( int16 *reA, int16 *imA, int16 *phaseA, uint16 sizeA )
{
	for( ; sizeA--; )
	{
		int16 reL = *reA++;
		int16 imL = *imA++;
		int16 phL = 0;
		
		if( reL < 0 )
		{
			reL = -reL;
			if( imL < 0 )
			{
				imL = -imL;
				if( reL > imL ) 
				{
					phL = -32768 + bbs_atan2( imL, reL );
				}
				else
				{
					phL = -16384 - bbs_atan2( reL, imL );
				}
			}
			else
			{
				if( reL > imL ) 
				{
					phL = -( -32768 + bbs_atan2( imL, reL ) );
				}
				else
				{
					if( imL == 0 ) phL = 0;
					else phL = 16384 + bbs_atan2( reL, imL );
				}
			}
		}
		else
		{
			if( imL < 0 )
			{
				imL = -imL;
				if( reL > imL ) 
				{
					phL = -bbs_atan2( imL, reL );
				}
				else
				{
					phL = -16384 + bbs_atan2( reL, imL );
				}
			}
			else
			{
				if( reL > imL ) 
				{
					phL = bbs_atan2( imL, reL );
				}
				else
				{
					if( imL == 0 ) phL = 0;
					else phL = 16384 - bbs_atan2( reL, imL );
				}
			}
		}
		
		*phaseA++ = phL;
	}
	return 0;
}

#endif

/* ------------------------------------------------------------------------- */
