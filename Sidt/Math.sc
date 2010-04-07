int log2(int value) {
	int ret = 0;
	while(value > 1) {
		++ret;
		value >>= 1;
	}
	return ret;
}
