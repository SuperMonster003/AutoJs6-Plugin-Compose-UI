"ui";

let refreshing = compose.state(false);

let rows = [];
for (let i = 0; i < 30; i++) rows.push(compose.Text({key: 'row-' + i, text: 'Pull down to refresh ' + i}));

compose.mount(function () {
    return (
        <compose.PullToRefresh key='refresh' refreshing={refreshing.value} onRefresh={function () { refreshing.value = true; setTimeout(function () { refreshing.value = false; }, 1500); }}>
            <compose.LazyColumn contentPadding={16} spacing={8}>
                {rows}
            </compose.LazyColumn>
        </compose.PullToRefresh>
    );
});
