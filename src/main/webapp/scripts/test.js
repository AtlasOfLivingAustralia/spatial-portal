    <html>
    <body>

    <canvas id="c1" width="100%" height="100%"></canvas>
    <script type="text/javascript">
    document.getElementById("c1").getContext("2d").fillStyle="#FF0000";
document.getElementById("c1").getContext("2d").fillRect(10,10,200,200);
    </script>

    <script type="text/javascript">

   function Data() {
        this.grid = [];
        this.rowNames = [];
        this.colNames = [];
        this.extrinsicRows = [];
        this.extrinsicColumns = [];
        
        this.rowIdx = [];
        this.columnIdx = [];
        this.rowCount = 0;
        this.columnCount = 0;
        
        this.missingValue = -9999;
    }

Data.prototype.readCSV = function(text) {
    var rows = text.split('\n');
    for(var i=0;i<rows.length;i++) {
        var row = rows[i].split(',');
        if(i == 0) {
            for(var j=1;j<row.length;j++) {
                this.colNames[j-1] = row[j];
                this.extrinsicColumns[j-1] = false;
            }
        } else {
            this.grid[i-1] = [];
            for(var j=0;j<row.length;j++) {
                if(j == 0) {
                    this.rowNames[i-1] = row[0];
                    this.extrinsicRows[i-1] = false;
                } else {
                    this.grid[i - 1][j-1] = row[j];
                }
            }
        }
    }	
    this.updateRowIdxs();
    this.updateColumnIdxs();
}

Data.prototype.setExtrinsicRow = function(row) {
    this.extrinsicRows[row] = true;
    this.updateRowIdxs();
}

Data.prototype.setExtrinsicColumn = function (column) {
    this.extrinsicColumn[column] = true;
    this.updateColumnIdxs();
}

Data.prototype.updateRowIdxs = function() {
    this.rowCount = 0;
    for(var i=0;i<this.grid.length;i++) {
        if(!this.extrinsicRows[i]) {
            this.rowIdx[this.rowCount] = i;
            this.rowCount = this.rowCount + 1;
        }
    }
}

Data.prototype.updateColumnIdxs = function() {	
    this.columnCount = 0;
    if (this.grid.length > 0) {
        for(var i=0;i<this.grid[0].length;i++) {
            if(!this.extrinsicColumns[i]) {
                this.columnIdx[this.columnCount] = i;
                this.columnCount = this.columnCount + 1;
            }
        }
    }
}

Data.prototype.getAt = function(row,column) {
    return this.grid[this.rowIdx[row]][this.columnIdx[column]] * 1;
}

Data.prototype.dump = function() {
    var s = "";
    for(var i=0;i<this.colNames.length;i++) {
        s += "," + this.colNames[i];
    }
    for(var i=0;i<this.rowNames.length;i++) {
        s += "\n" + this.rowNames[i];
        for(var j=0;j<this.colNames.length;j++) {
            s += "," + this.grid[i][j];
        }		
    }	
    return s;	
}

function Association() {
    this.grid = [];
    this.rowNames = [];		
}

Association.prototype.run = function(project) {
    this.rowNames = project.data.rowNames;
    var data = project.data;
    for(var i=1;i<data.rowCount;i++) {
        this.grid[i] = [];
        for(var k=0;k<i;k++) {
            var numerator = 0;
            var denominator = 0;
            for(var j=0;j<data.columnCount;j++) {
                var v1 = data.getAt(i,j);
                var v2 = data.getAt(k,j);
                if(v1 != data.missingValue && v2 != data.missingValue) {
                    numerator += Math.abs(v1 - v2);
                    denominator += Math.abs(v1 + v2);
                }
            }
            if(denominator > 0) {
                this.grid[i][k] = numerator / denominator;
            } else {
                this.grid[i][k] = undefined;
            }
        }
    }
}

Association.prototype.dump = function() {
    var s = "";
    for(var i=0;i<this.rowNames.length - 1;i++) {	
        s += "," + this.rowNames[i];
    }
    for(var i=1;i<this.rowNames.length;i++) {	
        s += "\n" + this.rowNames[i];
        for(var k=0;k<i;k++) {
            s += "," + this.grid[i][k];
        }
    }
    return s;
}

function Ordination() {
    this.data = [];
    this.dim = 3;
    this.seed = 12345;
    this.starts = 10;
    this.iter = 100;
    this.cut = 0.75;
	
    this.sort_count;
    this.max_sorts;
}

    Ordination.prototype.run = function(project) {
	this.k = project.association.data.length + 1, p = (k * (k-1)) / 2.0, n = k;
	this.i = 0;
	this.j = 0;
	this.l = 0;

	//setup ssh inputs
	var dim = [], dout = [], dest = []
	var conf = [], grad = [], gral = [], saveconf = [];
	for (i=0;i<n;i++) {
            conf[i] = [];
            gard[i] = [];
            gral[i] = [];
            saveconf[i] = [];
	}
	var indx = [];
	var size = this.dim;
	
	for(i=0;i<p;i++){
            dout[i] = 0;
            dest[i] = 0;
            indx[i] = 0;
	}
	p = 0;
	for(i=0;i<project.association.data.length;i++){
            for(k=0;k<project.association.data[i].length;k++){
                din[p] = project.association.data[i][k];
                p++;
            }
	}

	this.stress = 0;

        //	if(ssh.Ssh1(din,dout,dest,indx,conf,grad,gral,size,n,
        //			m_pDoc,cut,m_pDoc->m_OrdIterations,stress,saveconf,
        //			m_pDoc->m_OrdRandomStarts,m_pDoc->m_OrdSeed,p)) 

	var nd = this.dim;
	var cut = this.cut;
	var pMaxIterations = this.iter;
	var pNumberOfRandomStarts = this.starts;
	var pRandomSeed = this.seed;
	var assocsize = p;

	sort_count = 0;	
	max_sorts = pMaxIterations*pNumberOfRandomStarts + 2;

	var _small = 0.000001;
	var stbest = 1.0;
	var itnum = -1;
	//STARTNEW
	var itnum = -1;
	//ENDNEW
	var irs = 1;
	var fn = n;
	var sqrtn = Math.sqrt(fn*1.0);
	var sqrnd = Math.sqrt(1.0/nd);
	var nbak = 0;
	var ult = false;
	var maxit = pMaxIterations;
	var strdif = 0.005;
	var ireg = 2;

	var i92 = (n*(n-1))/2;
	var icut;
	var done;
	var nord;
	var nrs;

	//sort din & locate cut
	indx[0] = -1;

	this.g_it = 0;
	this.g_stress = 1;

	icut = i92;
	done = false;
	for(i=0;i<i92 && !done;i++){
            if(din[i] > cut){
                icut = i;
                done = true;
            }
	}

	//rank ordinals
	if(icut < i92){
            nord = i92 - icut + 1;
            Rank(din+icut,nord);
	}else{
            sort_count++;
	}

	var ntunnels = 0;

	var sfgr = sqrtn;
	var acsav = 0;
	var cosav = 0;
	var sratav = 0.8;
	var stlast = 1;
	var step = 0;
	this.r  = 0.111111;

	this.x0 = 0; this.x1 = 0; this.u = 0; this.t = 0;
	var rns;
	var sum;

	var angle;
	var rmean;
	var sfgl;
	var stress;
	var ut;
	var uts;
	var dk;
	var scale;
	var shift;
	var cosg;
	var gil;
	var srat;
	var stb;
	var step0;
	var relax;	
	var goodl;
	var dev;

	var goto_NineZeroOne = true;
	var goto_TwoZeroZero = false;
	while (goto_NineZeroOne || gotoTwoZeroZero ) {
            goto_NineZeroOne = false;

            if (!goto_TwoZeroZero) {

                //initial configuration
                sfgr = sqrtn;
                acsav = 0;
                cosav = 0; 
                sratav = 0.8;
                stlast = 1;
                step = 0;
                r  = 0.111111;
	
                //internal uniform random - default
                if(itnum == -1){
                    nrs = pNumberOfRandomStarts;
                    rns = pRandomSeed;		
                }else{
                    irs++;
                    rns++;
                }
		
                //force sort_count
                sort_count = (irs-1)*maxit+2;
	
                for(i=0;i<n;i++){
                    for(j=0;j<nd;j++){
			conf[i][j] = Math.rand()*2.0-1.0;
                    }
                }

                //initialize gradient
                for(i=0;i<n;i++){
                    for(l=0;l<nd;l++){
			grad[i][l] = sqrnd;
                    }
                }
	
                g_it = irs;
                g_stress = stbest;
                stress = 1;
                //iteration
                for(itnum=0;itnum<maxit && !goto_TwoZeroZero;itnum++){
                    g_it = irs;
		
                    if(g_stress > stress){
			stbest = stress;
			g_stress = stress;
                    }
		
                    //normalize config + ini & transfer
                    dev = 0;
                    for(l=0;l<nd;l++){
			sum = 0;
			for(i=0;i<n;i++){
                            sum += conf[i][l];
			}
			rmean = sum / fn;
			for(i=0;i<n;i++){
                            conf[i][l] = conf[i][l] - rmean;
                            dev += Math.pow(conf[i][l],2);
			}
                    }

                    // / by sd & ready grad-gral
                    dev = Math.sqrt(dev/fn);
                    for(i=0;i<n;i++){
			for(l=0;l<nd;l++){
                            conf[i][l] = conf[i][l] / dev;
                            gral[i][l] = grad[i][l] / dev;
                            grad[i][l] = 0.0;
			}
                    }
                    sfgl = sfgr / dev;

                    //euclidean on config
                    Ed(conf,dout,indx,i92,n,nd);

                    //regressions
                    if(icut > 0)
			LinReg(din,dout,i92,icut,ireg,this.r,this.x0,this.x1);
                    if(icut < i92-1){
			OrdReg(dout+icut,dest+icut,nord);
                    }else{
			sort_count++;
                    }

                    //estimates & disparities & stress
                    Fit(din,dout,dest,i92,icut,ireg,x0,x1,u,t);
                    stress = Math.sqrt(u/t);
                    //	stimp = (stlast-stress)/stlast;
                    ut = Math.sqrt(u) / (t * Math.sqrt(t));
                    uts = Math.sqrt(u * t);

                    var dist1, dist2;
                    //calculate gradient grad
                    for(k=0;k<i92;k++){
			this.irow = i; this.icol = k;
			LinLt(indx[k]);
			dk = dout[k];
			if(dk != 0 && uts != 0){
                            scale = stress*((ut*dk)-(dk-dest[k])/uts)/dk;
                            for(l=0;l<nd;l++){
                                shift = scale*(conf[i][l] - conf[j][l]);
                                dist1 = grad[i][l];
                                dist2 = grad[j][l];
                                grad[i][l] = dist1 + shift;
                                grad[j][l] = dist2 - shift;
                            }
			}else{
                            scale = stress;
			}
                    }

                    //scale factor & grad-gral
                    sfgr = 0;
                    cosg = 0;
                    for(i=0;i<n;i++){
			for(l=0;l<nd;l++){
                            gil = grad[i][l];
                            sfgr = sfgr + gil * gil;
                            cosg = cosg + gil * gral[i][l];
			}
                    }
                    sfgr = Math.sqrt(sfgr / fn);

                    //if sfgr < small, write & stop
                    if(sfgr < _small){
			goto_TwoZeroZero = true;
                    } else {
                        //go on
                        cosg = cosg / (sfgl*sfgr*fn);
                        if(itnum == 0) 
                            srat = 0.8;
                        else 
                            srat = stress / stlast;
	
                        //overshoot
                        if(cosg < -0.95 || srat > 1.1){
                            nbak++;
                            if(nbak == 1) stb = 1.0;
                            stb = stb / dev;
                            step0 = step;
                            step = step / 20.0;
                            step0 = stb*(step0-step)/sfgl;
                            for(i=0;i<n;i++){
				for(l=0;l<nd;l++){
                                    conf[i][l] = conf[i][l] - 
                                        step0 * gral[i][l];
                                    grad[i][l] = gral[i][l];
				}
                            }
                            sfgr = sfgl;
                            stress = stlast;
                        }else{
                            step0 = step;
                            nbak = 0;
                            sratav = Math.pow(srat,0.33334) * Math.pow(sratav,0.66666);
                            cosav = cosg*0.66 + cosav*0.34;
                            acsav = Math.abs(cosg)*0.66 + acsav*0.34;
                            if(itnum == 0){
				step = 25.0*stress*sfgr;
                            }else{
				angle = Math.pow(4.0,cosav);
				relax = 1.6/((1+Math.pow(min(1.0,sratav),5.0))*(1+(acsav - Math.abs(cosav))));
				goodl = Math.sqrt(min(1.0,srat));
				step = step * angle * relax * goodl;
                            }
                            //don't let step blow out
                            if(step > 9) step = 1;
			
                            //stopping rules
                            if((srat > (1-strdif) && srat < 1) &&
				(sratav > (1-10*strdif) && sratav < 1)){
				goto_TwoZeroZero = true;
                            }else if(itnum >= maxit){
				goto_TwoZeroZero = true;
                            }
		
                            if(!goto_TwoZeroZero) {
                                //continue: move points
                                shift = step / sfgr;
                                for(i=0;i<n;i++){
                                    for(l=0;l<nd;l++){
					conf[i][l] = conf[i][l] + grad[i][l] * shift;
                                    }
                                }
                            }
                        }
                        if(!goto_TwoZeroZero) {
                            stlast = stress;
                        }
                    }
                }

            }

            goto_TwoZeroZero = false;

            //FOR PROGRESSBAR
	
            if(stress < stbest){
		//save it all
		stbest = stress;
		pStress = stress;
		for(i=0;i<n;i++){
                    for(l=0;l<nd;l++){
                        saveconf[i][l] = conf[i][l];
                    }
		}
            }
	
            //more rands?
            if(irs < nrs)
		goto_NineZeroOne = true;
	
	}
    }

    Ordination.prototype.Ed = function(conf, ass, indx, i92, n, nd) {
	var i,j;
	var dif,dist1,dist2;
	var l;

	for(var k=0;k<i92;k++){
            this.irow = i;
            this.icol = j;
            LinLt(indx[k]);
            ass[k] = 0;
            for(l=0;l<nd;l++){
                dist1 = conf[i][l];
                dist2 = conf[j][l];
                dif = dist1 - dist2;
                ass[k] = ass[k] + dif*dif;
            }
            ass[k] = sqrt(ass[k]);
	}
    }

    Ordination.prototype.OrdReg = function(dout, dest, nord) {
	//copy dout to dest
	var i;
	for(i=0;i<nord;i++){
            dest[i] = dout[i];
	}

	SrtShl(dest,NULL,nord);
    }

    Ordination.prototype.Fit = function(din, dout, dest, i92, icut, ireg, x0, x1) {
	var i;

	this.u = 0;
	this.t = 0;
	
	if(icut > 1){
            if(ireg == 0){
                for(i=0;i<icut;i++){
                    dest[i] = x0+x1*din[i];
                    u = u + Math.pow(dout[i] - dest[i],2);
                    t = t + dout[i]*dout[i];
                }
            }else{
                for(i=0;i<icut;i++){
                    dest[i] = x1*din[i];
                    u = u + Math.pow(dout[i] - dest[i],2);
                    t = t + dout[i]*dout[i];
                }
            }
            if(icut < i92){
                for(i=icut;i<i92;i++){
                    u = u + Math.pow(dout[i] - dest[i],2);
                    t = t + dout[i]*dout[i];
                }
            }
	}
    }

    Ordination.prototype.Rank = function(data, n) {
	var sumrnk;
	var sum;
	var done;
	var i, j;
	
	SrtShl(data,NULL,n);
		
	for(i=0;i<n-1;i++){
            if(data[i] == data[i+1]){
                sum = 2.0;
                sumrnk = i+i+1+2;
                done = false;
                for(j=i+2;j<n && !done;j++){
                    if(data[i] == data[j]){
                        sum++;
                        sumrnk += j+1;
                    }else{
                        i = Rank2(data,i,j-1,sumrnk,sum);
                        done = true;
                    }
                }
                if(!done){
                    i = Rank2(data,i,n-1,sumrnk,sum);
                    return;
                }
            }else{
                data[i] = i+1;
            }
	}
	data[i] = i+1;
    }

    Ordination.prototype.Rank2 = function(data, i1, i2, sumrnk, sum) {
	var i, rnkav = sumrnk/sum;
	for(i=i1;i<=i2;i++) data[i] = rnkav;
	return i2;
    }

    Ordiantoin.prototype.SrtShl = function(data, indx, nj) {
	sort_count++;

	var i,j,fm,ii;
	//setup index
	if(n == 0){
            if(indx != NULL) indx[0] = 1;
            return;
	}else if(indx != NULL && indx[0] == -1){
            for(i=0;i<n;i++){
                indx[i] = i;
            }
	}

	var fn = n;
	var nloops = Math.log(fn*1.0)/Math.log(2.0);
	var m = Math.pow(2.0,nloops-1);
	var tempd;
	var tempi;
	
	var progress = 0;
	var step = Math.max(1,n-m) * Math.max(1,nloops) / 100*2;

	for(ii=1;ii<=Math.max(1,nloops);ii++){
            fm = m;
            for(i=1;i<=Math.max(1,n-m);i++){
                if(progress == step){
                    progress = 0;
                }else{
                    progress++;
                }

                if(data[i-1] > data[i+m-1]){
                    tempd = data[i+m-1];
                    if(indx != NULL) tempi = indx[i+m-1];
                    data[i+m-1] = data[i-1];
                    if(indx != NULL) indx[i+m-1] = indx[i-1];
                    if(i <= m){
                        data[i-1] = tempd;
                        if(indx != NULL) indx[i-1] = tempi;
                    }else{
                        for(j=i-m;j>=1;j=j - m){
                            if(tempd >= data[j-1]) {
                                break;
                            }
                            data[j+m-1] = data[j-1];
                            if(indx != NULL) indx[j+m-1] = indx[j-1];
                        }
                        data[j+m-1] = tempd;
                        if(indx != NULL) indx[j+m-1] = tempi;
                    }
                }
            }
            m = fm/2;
	}
    }


    Ordination.prototype.LinReg = function(x,y,n,icut,ireg,r,x0,x1) {
	var _small = 0.00001;
	var sx = 0.0;
	var sxx = sx;
	var sy = sx;
	var syy = sx;
	var sxy = sx;
	var fn = icut;
	var y0,y1;
	var i;
	
	var valmis = this.missingvalue;

	//sums & sumosquares
	for(i=0;i<icut;i++){
            if(x[i] != valmis && y[i] != valmis){
                sx += x[i];
                sxx += x[i]*x[i];
                sy += y[i];
                syy += y[i]*y[i];
                sxy += x[i]*y[i];
            }else{
                fn--;
            }
	}
     
	var sxn = fn*sxx-sx*sx;
	var syn = fn*syy-sy*sy;
	var sxyn = fn*sxy-sx*sy;

	var sxnsyn = Math.sqrt(sxn*syn);
	if(sxnsyn > _small){
            this.r = sxyn/sxnsyn;
	}else if(sxyn > _small){
            this.r = 0.0;
	}else{
            this.r = 1.0;
	}

	this.x0 = 0.0;
	y0 = 0.0;
	this.x1 = sxy/sxx;
	y1 = sxy/syy;
    }


    Ordination.prototype.SrtSh1 = function(data, n) {
	var ii,fm,i;
	var j;
	var fn = n;
	var nloops = Math.log(fn*1.0)/log(2.0);
	var m = Math.pow(2.0,nloops-1);
	var tempd;

	for(ii=1;ii<=Math.max(1,nloops);ii++){
            fm = m;
            for(i=1;i<=Math.max(1,n-m);i++){
                if(data[i-1] > data[i+m-1]){
                    tempd = data[i+m-1];
                    data[i+m-1] = data[i-1];
                    if(i <= m){
                        data[i-1] = tempd;
                    }else{
                        for(j=i-m;j>=1;j-=m){
                            if(tempd >= data[j-1]) 
                                break;

                            data[j+m-1] = data[j-1];
                        }
                        data[j+m-1] = tempd;
                    }
                }
            }
            m = fm/2;
	}
    }

    Ordination.prototype.LinLt = function(positn) {
	positn++;
	this.irow = Math.sqrt(2*positn+0.25)+0.499;
	this.icol = (positn - (this.irow*(this.irow-1)/(2.0)));
	this.icol = this.icol - 1;
    }

    function Project () {
        this.data = new Data();
        this.association = new Association();
        this.classification = new Classification();
        this.ordination = new Ordination();
    }


        </script>

        </body>
        </html>b