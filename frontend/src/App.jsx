import { startTransition, useEffect, useMemo, useState } from "react";

const COLORS = ["#3b82f6", "#14b8a6", "#f59e0b", "#ef4444", "#8b5cf6", "#06b6d4"];

function App() {
  const [summary, setSummary] = useState(null);
  const [schools, setSchools] = useState([]);
  const [plans, setPlans] = useState([]);
  const [allPayments, setAllPayments] = useState([]);
  const [schoolPayments, setSchoolPayments] = useState([]);
  const [allInvoices, setAllInvoices] = useState([]);
  const [visibleInvoices, setVisibleInvoices] = useState([]);
  const [selectedSchoolCode, setSelectedSchoolCode] = useState("");
  const [invoiceFilterSchool, setInvoiceFilterSchool] = useState("ALL");
  const [message, setMessage] = useState("Loading CRM dashboard...");

  useEffect(() => {
    loadDashboard();
  }, []);

  useEffect(() => {
    if (!selectedSchoolCode) {
      return;
    }
    loadSchoolPayments(selectedSchoolCode);
  }, [selectedSchoolCode]);

  useEffect(() => {
    if (invoiceFilterSchool === "ALL") {
      setVisibleInvoices(allInvoices);
      return;
    }
    setVisibleInvoices(allInvoices.filter((invoice) => invoice.schoolCode === invoiceFilterSchool));
  }, [invoiceFilterSchool, allInvoices]);

  const selectedSchool = useMemo(
    () => schools.find((school) => school.schoolCode === selectedSchoolCode) ?? null,
    [schools, selectedSchoolCode]
  );

  const activeSchools = useMemo(
    () => schools.filter((school) => school.status === "ACTIVE"),
    [schools]
  );

  const planDistribution = useMemo(() => {
    const counts = activeSchools.reduce((accumulator, school) => {
      const key = school.planName || "Unassigned";
      accumulator[key] = (accumulator[key] || 0) + 1;
      return accumulator;
    }, {});

    return Object.entries(counts).map(([label, count], index) => ({
      label,
      count,
      color: COLORS[index % COLORS.length]
    }));
  }, [activeSchools]);

  const combinedPlanData = useMemo(() => {
    const totalRevenue = allPayments
      .filter((p) => p.paymentStatus === "SUCCESS")
      .reduce((sum, p) => sum + Number(p.totalAmount || p.amount || 0), 0);

    const stats = plans.map((plan, index) => {
      const schoolsCount = activeSchools.filter((s) => s.planName === plan.name).length;
      const revenueTotal = allPayments
        .filter((p) => p.paymentStatus === "SUCCESS" && p.planName === plan.name)
        .reduce((sum, p) => sum + Number(p.totalAmount || p.amount || 0), 0);
      
      const share = totalRevenue > 0 ? (revenueTotal / totalRevenue) * 100 : 0;
      
      return {
        label: plan.name,
        count: schoolsCount,
        value: revenueTotal,
        share: share,
        color: COLORS[index % COLORS.length]
      };
    });

    // Find top performer
    const topPlan = [...stats].sort((a, b) => b.value - a.value)[0];
    return { stats, totalRevenue, topPlan: topPlan?.value > 0 ? topPlan.label : null };
  }, [activeSchools, allPayments, plans]);

  const crmMetrics = useMemo(() => {
    const planCount = planDistribution.length;
    const activeRate = schools.length ? Math.round((activeSchools.length / schools.length) * 100) : 0;
    return [
      {
        label: "Total Active Schools",
        value: activeSchools.length,
        note: `${activeRate}% of overall school base`,
        tone: "blue",
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M3 21h18M3 10l9-7 9 7M5 10v11M19 10v11M9 21v-4a2 2 0 012-2h2a2 2 0 012 2v4" />
          </svg>
        )
      },
      {
        label: "Total Revenue",
        value: formatCurrency(summary?.totalRevenue),
        note: "Collected across successful subscriptions",
        tone: "teal",
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M12 1v22M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6" />
          </svg>
        )
      },
      {
        label: "Current Month Revenue",
        value: formatCurrency(summary?.currentMonthRevenue),
        note: "Month to date collections",
        tone: "violet",
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
            <line x1="16" y1="2" x2="16" y2="6" />
            <line x1="8" y1="2" x2="8" y2="6" />
            <line x1="3" y1="10" x2="21" y2="10" />
          </svg>
        )
      },
      {
        label: "Subscription Plan Types",
        value: planCount,
        note: "Plan-wise school distribution",
        tone: "amber",
        icon: (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
          </svg>
        )
      }
    ];
  }, [activeSchools.length, planDistribution.length, schools.length, summary]);

  async function fetchJson(url) {
    const response = await fetch(url);
    const payload = await response.json();
    if (!response.ok) {
      throw new Error(payload.message || "Request failed");
    }
    return payload.data;
  }

  async function loadDashboard() {
    try {
      const [summaryData, schoolData, planData, paymentData, invoiceData] = await Promise.all([
        fetchJson("/api/schools/summary"),
        fetchJson("/api/schools"),
        fetchJson("/api/schools/plans/all"),
        fetchJson("/api/schools/payments/all"),
        fetchJson("/api/invoices")
      ]);

      startTransition(() => {
        setSummary(summaryData);
        setSchools(schoolData);
        setPlans(planData);
        setAllPayments(paymentData);
        setAllInvoices(invoiceData);
        setVisibleInvoices(invoiceData);
      });

      if (schoolData.length > 0) {
        setSelectedSchoolCode((current) => current || schoolData[0].schoolCode);
      }

      setMessage("CRM dashboard refreshed successfully.");
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function loadSchoolPayments(schoolCode) {
    try {
      const payments = await fetchJson(`/api/schools/${schoolCode}/payments`);
      setSchoolPayments(payments);
      setMessage(`Showing payment history for ${schoolCode}.`);
    } catch (error) {
      setMessage(error.message);
    }
  }

  function downloadInvoice(invoiceNumber) {
    window.open(`/api/invoices/${invoiceNumber}/download`, "_blank", "noopener,noreferrer");
  }

  return (
    <div className="crm-shell">
      <main className="main-area">
        <header className="topbar">
          <div className="brand-section">
            <div className="brand-mark">SE</div>
            <div className="brand-text">
              <span className="brand-label">School ERP CRM</span>
              <strong>Revenue Command</strong>
            </div>
          </div>
          <div className="topbar-actions">
            <button className="refresh-button" type="button" onClick={loadDashboard}>
              Refresh Dashboard
            </button>
          </div>
        </header>

        <section className="metric-grid">
          {crmMetrics.map((metric) => (
            <article key={metric.label} className={`metric-card tone-${metric.tone}`}>
              <div className="metric-icon">{metric.icon}</div>
              <div className="metric-content">
                <span>{metric.label}</span>
                <strong>{metric.value}</strong>
                <small>{metric.note}</small>
              </div>
            </article>
          ))}
        </section>

        <section className="dashboard-grid">
          <article className="panel panel-distribution">
            <div className="panel-header">
              <div>
                <h2>Subscribed Plan Economics</h2>
                <p className="panel-note">Distribution of active schools vs revenue performance</p>
              </div>
              {combinedPlanData.topPlan && (
                <div className="status-badge pulse">
                  Top Driver: {combinedPlanData.topPlan}
                </div>
              )}
            </div>
            <div className="industry-visual">
              <div className="distribution-focus">
                <RadialChart data={combinedPlanData.stats} mode="count" title="Schools" />
              </div>
              <div className="performance-list">
                {combinedPlanData.stats.map((item) => (
                  <div key={item.label} className="perf-item">
                    <div className="perf-header">
                      <div className="perf-label">
                        <span className="legend-dot" style={{ backgroundColor: item.color }} />
                        <strong>{item.label}</strong>
                      </div>
                      <div className="perf-meta">
                        <span>{item.count} Schools</span>
                        <strong>{formatCurrency(item.value)}</strong>
                      </div>
                    </div>
                    <div className="perf-track">
                      <div 
                        className="perf-fill" 
                        style={{ 
                          width: `${item.share}%`, 
                          backgroundColor: item.color,
                          boxShadow: `0 0 12px ${item.color}44`
                        }} 
                      />
                    </div>
                    <div className="perf-footer">
                      <span>{Math.round(item.share)}% Revenue Share</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </article>

          <article className="panel panel-monthly">
            <div className="panel-header">
              <div>
                <h2>Revenue Monthwise</h2>
                <p className="panel-note">Monthly collection trend from successful payments</p>
              </div>
            </div>
            <RevenueBars data={summary?.monthlyTrend ?? []} />
          </article>


          <article className="panel panel-active-schools">
            <div className="panel-header">
              <div>
                <h2>Total Active School List</h2>
                <p className="panel-note">Compact list of currently active schools</p>
              </div>
            </div>
            <div className="table-wrap panel-scroll">
              <table>
                <thead>
                  <tr>
                    <th>School</th>
                    <th>Board</th>
                    <th>Plan</th>
                    <th>License End</th>
                  </tr>
                </thead>
                <tbody>
                   {activeSchools.map((school) => (
                    <tr key={school.schoolCode}>
                      <td>
                        <div className="school-identity">
                          <strong>{school.name}</strong>
                          <span className="school-code">{school.schoolCode}</span>
                        </div>
                      </td>
                      <td>{school.board || "-"}</td>
                      <td>{school.planName || "-"}</td>
                      <td>{formatDate(school.licenseEndDate)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </article>

          <article className="panel panel-history">
            <div className="panel-header">
              <div>
                <h2>Payment History & Invoices</h2>
                <p className="panel-note">Consolidated audit trail with subscription periods and active invoices</p>
              </div>
              <select
                className="panel-select-compact"
                value={selectedSchoolCode || "ALL"}
                onChange={(e) => {
                  const code = e.target.value === "ALL" ? "" : e.target.value;
                  setSelectedSchoolCode(code);
                  setInvoiceFilterSchool(e.target.value);
                }}
              >
                <option value="ALL">Select School...</option>
                {schools.map((school) => (
                  <option key={school.schoolCode} value={school.schoolCode}>
                    {school.name} ({school.schoolCode})
                  </option>
                ))}
              </select>
            </div>
            <div className="table-wrap panel-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Order</th>
                    <th>Plan</th>
                    <th>Status</th>
                    <th>Total</th>
                    <th>Subscription Period</th>
                    <th>Invoice</th>
                  </tr>
                </thead>
                <tbody>
                  {schoolPayments.length === 0 ? (
                    <tr>
                      <td colSpan="6">No payment history available for the selected school.</td>
                    </tr>
                  ) : (
                    schoolPayments.map((payment) => (
                      <tr key={payment.id} className="compact-row">
                        <td>{payment.orderId}</td>
                        <td>{payment.planName || "-"}</td>
                        <td>
                          <span className={`status-pill ${payment.paymentStatus?.toLowerCase()}`}>
                            {payment.paymentStatus}
                          </span>
                        </td>
                        <td>{formatCurrency(payment.totalAmount || payment.amount)}</td>
                        <td>
                          {payment.subscriptionStartDate ? (
                            <div className="period-box">
                              <span>{formatDate(payment.subscriptionStartDate)}</span>
                              <span className="arrow">→</span>
                              <span>{formatDate(payment.subscriptionEndDate)}</span>
                            </div>
                          ) : "-"}
                        </td>
                        <td>
                           {payment.invoiceNumber ? (
                             <div className="invoice-action">
                               <span className="inv-num">{payment.invoiceNumber}</span>
                               <button 
                                 className="mini-download" 
                                 title="Download Invoice"
                                 onClick={() => downloadInvoice(payment.invoiceNumber)}
                               >
                                 <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                                   <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                                   <polyline points="7 10 12 15 17 10" />
                                   <line x1="12" y1="15" x2="12" y2="3" />
                                 </svg>
                               </button>
                             </div>
                           ) : "-"}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </article>

          <article className="panel panel-plans">
            <div className="panel-header">
              <div>
                <h2>Subcription Offering</h2>
                <p className="panel-note">Review active pricing tires and plan benefits</p>
              </div>
            </div>
            <div className="pricing-grid panel-scroll">
              {plans.map((plan, index) => {
                const isPopular = plan.name === "Growth";
                return (
                  <div key={plan.id} className={`pricing-card ${isPopular ? "popular" : ""}`}>
                    {isPopular && <div className="popular-badge">Most Popular</div>}
                    <div className="card-top">
                      <span className="plan-dot" style={{ backgroundColor: COLORS[index % COLORS.length] }} />
                      <h3>{plan.name}</h3>
                    </div>
                    <div className="price-tag">
                      <strong>{formatCurrency(plan.price)}</strong>
                      <span>/ {plan.durationInMonths} Months</span>
                    </div>
                    <ul className="feature-list">
                      <li>
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                          <polyline points="20 6 9 17 4 12" />
                        </svg>
                        Full System Access
                      </li>
                      <li>
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                          <polyline points="20 6 9 17 4 12" />
                        </svg>
                        {plan.name === "Starter" ? "Basic Support" : "Priority Support"}
                      </li>
                      <li>
                         <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                          <polyline points="20 6 9 17 4 12" />
                        </svg>
                        Automatic Backups
                      </li>
                      {plan.name !== "Starter" && (
                        <li>
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                            <polyline points="20 6 9 17 4 12" />
                          </svg>
                          API Integrations
                        </li>
                      )}
                    </ul>
                  </div>
                );
              })}
            </div>
          </article>
        </section>
      </main>
    </div>
  );
}

function RadialChart({ data, mode, title }) {
  if (!data.length) {
    return <div className="empty-state">No data available.</div>;
  }

  const total = data.reduce((sum, item) => sum + (mode === "count" ? item.count : item.value), 0);
  if (total === 0) return <div className="empty-state">No data.</div>;

  let currentAngle = 0;
  const segments = data
    .map((item) => {
      const val = mode === "count" ? item.count : item.value;
      const angle = (val / total) * 360;
      const segment = `${item.color} ${currentAngle}deg ${currentAngle + angle}deg`;
      currentAngle += angle;
      return segment;
    })
    .join(", ");

  return (
    <div className="radial-block">
      <div className="donut-wrap">
        <div className="donut-chart" style={{ background: `conic-gradient(${segments})` }}>
          <div className="donut-hole">
            <strong>{mode === "count" ? total : formatCurrency(total)}</strong>
            <span>{title}</span>
          </div>
        </div>
      </div>
    </div>
  );
}

function RevenueBars({ data }) {
  if (!data.length) {
    return <div className="empty-state">No monthly revenue data available.</div>;
  }

  const maxValue = Math.max(...data.map((item) => Number(item.revenue || 0)), 1);

  return (
    <div className="vertical-chart">
      {data.map((item, index) => (
        <div key={item.label} className="vertical-bar-card">
          <div
            className="vertical-bar"
            style={{
              height: `${Math.max((Number(item.revenue || 0) / maxValue) * 150, 10)}px`,
              background: `linear-gradient(180deg, ${COLORS[index % COLORS.length]}, #0f172a)`
            }}
          />
          <strong>{item.label}</strong>
          <span>{formatCurrency(item.revenue)}</span>
        </div>
      ))}
    </div>
  );
}

function HorizontalBars({ data }) {
  if (!data.length) {
    return <div className="empty-state">No plan-wise revenue available.</div>;
  }

  const maxValue = Math.max(...data.map((item) => Number(item.value || 0)), 1);

  return (
    <div className="horizontal-chart">
      {data.map((item) => (
        <div key={item.label} className="horizontal-row">
          <div className="horizontal-meta">
            <strong>{item.label}</strong>
            <span>{formatCurrency(item.value)}</span>
          </div>
          <div className="horizontal-track">
            <div
              className="horizontal-fill"
              style={{
                width: `${(Number(item.value || 0) / maxValue) * 100}%`,
                background: `linear-gradient(90deg, ${item.color}, #0f172a)`
              }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}

function formatCurrency(amount) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2
  }).format(Number(amount || 0));
}

function formatDate(value) {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium"
  }).format(new Date(value));
}

export default App;
