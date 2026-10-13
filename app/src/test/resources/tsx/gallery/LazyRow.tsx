"ui";

let cards = [];
for (let i = 0; i < 20; i++) cards.push(compose.Card({key: 'card-' + i, padding: 16}, compose.Text({text: 'Card ' + i})));

compose.mount(function () {
    return (
        <compose.LazyRow contentPadding={16} spacing={8}>
            {cards}
        </compose.LazyRow>
    );
});
