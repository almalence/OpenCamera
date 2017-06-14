#pragma version(1)
#pragma rs java_package_name(net.sourceforge.opencamera)
#pragma rs_fp_relaxed

int32_t *histogram;

void init_histogram() {
	for(int i=0;i<256;i++)
		histogram[i] = 0;
}

void __attribute__((kernel)) histogram_compute(uchar4 in, uint32_t x, uint32_t y) {
	// We compute a histogram based on the max RGB value, so this matches with the scaling we do in histogram_adjust.rs.
	// This improves the look of the grass in testHDR24, testHDR27.
	uchar value = max(in.r, in.g);
	value = max(value, in.b);

	rsAtomicInc(&histogram[value]);
}
